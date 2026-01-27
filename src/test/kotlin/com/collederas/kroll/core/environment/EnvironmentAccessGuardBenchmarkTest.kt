package com.collederas.kroll.core.environment

import com.collederas.kroll.core.project.ProjectEntity
import com.collederas.kroll.core.project.ProjectRepository
import com.collederas.kroll.exceptions.EnvironmentNotFoundException
import com.collederas.kroll.exceptions.ForbiddenException
import com.collederas.kroll.user.AppUser
import com.collederas.kroll.user.AppUserRepository
import com.collederas.kroll.user.UserRole
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.hibernate.stat.Statistics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@TestPropertySource(
    properties = ["spring.jpa.properties.hibernate.generate_statistics=true", "logging.level.org.hibernate.stat=DEBUG"],
)
class EnvironmentAccessGuardBenchmarkTest {
    @Autowired
    lateinit var environmentRepository: EnvironmentRepository

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Autowired
    lateinit var appUserRepository: AppUserRepository

    @Autowired
    lateinit var environmentAccessGuard: EnvironmentAccessGuard

    @Autowired
    lateinit var entityManager: EntityManager

    private lateinit var owner: AppUser
    private lateinit var otherUser: AppUser
    private lateinit var project: ProjectEntity
    private lateinit var environment: EnvironmentEntity

    @BeforeEach
    fun setup() {
        environmentRepository.deleteAll()
        projectRepository.deleteAll()
        appUserRepository.deleteAll()

        owner =
            appUserRepository.save(
                AppUser(
                    email = "owner@example.com",
                    username = "owner",
                    passwordHash = "hash",
                    roles = setOf(UserRole.ADMIN),
                ),
            )

        otherUser =
            appUserRepository.save(
                AppUser(
                    email = "other@example.com",
                    username = "other",
                    passwordHash = "hash",
                    roles = setOf(UserRole.ADMIN),
                ),
            )

        project =
            projectRepository.save(
                ProjectEntity(
                    name = "Test Project",
                    owner = owner,
                ),
            )

        environment =
            environmentRepository.save(
                EnvironmentEntity(
                    name = "dev",
                    project = project,
                ),
            )

        entityManager.flush()
        entityManager.clear()
    }

    private fun getStatistics(): Statistics {
        val session = entityManager.delegate as Session
        return session.sessionFactory.statistics
    }

    @Test
    @Transactional
    fun `benchmark happy path - owner accessing environment`() {
        val stats = getStatistics()
        stats.clear()
        stats.isStatisticsEnabled = true

        environmentAccessGuard.requireOwner(environment.id, owner.id)

        val queryCount = stats.prepareStatementCount
        println("Happy Path Query Count: $queryCount")

        // Optimized: We expect 1 query.
        assertEquals(1, queryCount, "Expected 1 query, but got $queryCount")
    }

    @Test
    @Transactional
    fun `benchmark not found - environment does not exist`() {
        val stats = getStatistics()
        stats.clear()
        stats.isStatisticsEnabled = true

        assertThrows(EnvironmentNotFoundException::class.java) {
            environmentAccessGuard.requireOwner(UUID.randomUUID(), owner.id)
        }

        val queryCount = stats.prepareStatementCount
        println("Not Found Query Count: $queryCount")
        // Optimized: 2 queries (existsByIdAndProjectOwnerId + existsById)
        assertEquals(2, queryCount, "Expected 2 queries for Not Found case")
    }

    @Test
    @Transactional
    fun `benchmark forbidden - user is not owner`() {
        val stats = getStatistics()
        stats.clear()
        stats.isStatisticsEnabled = true

        assertThrows(ForbiddenException::class.java) {
            environmentAccessGuard.requireOwner(environment.id, otherUser.id)
        }

        val queryCount = stats.prepareStatementCount
        println("Forbidden Query Count: $queryCount")
        // Baseline: At least 2 queries (Fetch env, fetch project)
        assertTrue(queryCount >= 2, "Expected at least 2 queries for Forbidden case, but got $queryCount")
    }
}

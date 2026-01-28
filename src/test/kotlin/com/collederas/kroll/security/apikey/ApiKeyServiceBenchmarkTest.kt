package com.collederas.kroll.security.apikey

import com.collederas.kroll.core.environment.EnvironmentRepository
import com.collederas.kroll.core.project.ProjectRepository
import com.collederas.kroll.support.factories.EnvironmentFactory
import com.collederas.kroll.support.factories.ProjectFactory
import com.collederas.kroll.support.factories.UserFactory
import com.collederas.kroll.user.AppUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Session
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.Instant

@DataJpaTest
class ApiKeyServiceBenchmarkTest {
    @Autowired lateinit var apiKeyRepository: ApiKeyRepository

    @Autowired lateinit var environmentRepository: EnvironmentRepository

    @Autowired lateinit var projectRepository: ProjectRepository

    @Autowired lateinit var userRepository: AppUserRepository

    @Autowired lateinit var entityManager: TestEntityManager

    @Test
    fun `should retrieve api keys efficiently`() {
        // Setup dependencies
        val properties = ApiKeyConfigProperties()
        val service = ApiKeyService(apiKeyRepository, environmentRepository, properties)

        // Setup Data
        val user = userRepository.save(UserFactory.create())
        val project = projectRepository.save(ProjectFactory.create(owner = user))
        val env = environmentRepository.save(EnvironmentFactory.create(project = project))

        // Create 10 API Keys for this environment
        for (i in 1..10) {
            val key =
                ApiKeyEntity(
                    environment = env,
                    keyHash = "hash_$i",
                    mask = "mask_$i",
                    expiresAt = Instant.now().plusSeconds(3600),
                )
            apiKeyRepository.save(key)
        }

        entityManager.flush()
        entityManager.clear() // Clear cache to force DB hits

        // Enable Stats
        val session = entityManager.entityManager.unwrap(Session::class.java)
        val stats = session.sessionFactory.statistics
        stats.isStatisticsEnabled = true
        stats.clear()

        // Run Method
        val results = service.list(env.id)

        // Assert
        assertThat(results).hasSize(10)

        val queryCount = stats.queryExecutionCount
        assertThat(queryCount).isLessThanOrEqualTo(2)
    }
}

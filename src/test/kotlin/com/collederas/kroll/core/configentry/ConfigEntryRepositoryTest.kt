package com.collederas.kroll.core.configentry

import com.collederas.kroll.support.factories.ConfigEntryFactory
import com.collederas.kroll.support.factories.EnvironmentFactory
import com.collederas.kroll.support.factories.ProjectFactory
import com.collederas.kroll.support.factories.UserFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class ConfigEntryRepositoryTest {
    @Autowired private lateinit var configEntryRepo: ConfigEntryRepository

    @Autowired private lateinit var entityManager: TestEntityManager

    @Test
    fun `findActiveConfigs respects activeFrom and activeUntil`() {
        val now = Instant.parse("2025-01-01T00:00:00Z")
        val user = entityManager.persist(UserFactory.create())
        val project = entityManager.persist(ProjectFactory.create(owner = user))
        val env = entityManager.persist(EnvironmentFactory.create(project = project))

        val active =
            ConfigEntryFactory.create(
                environment = env,
                key = "active",
                activeFrom = now.minusSeconds(60),
                activeUntil = now.plusSeconds(60),
            )

        val future =
            ConfigEntryFactory.create(
                environment = env,
                key = "future",
                activeFrom = now.plusSeconds(60),
            )

        entityManager.persist(active)
        entityManager.persist(future)
        entityManager.flush()

        val result = configEntryRepo.findActiveConfigs(env.id, now)

        assertEquals(1, result.size)
        assertEquals("active", result.first().configKey)
    }
}

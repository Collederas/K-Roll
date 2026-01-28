package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.configentry.audit.ConfigEntryHistoryListener
import com.collederas.kroll.core.configentry.audit.ConfigEntryHistoryRepository
import com.collederas.kroll.core.configentry.entries.ConfigEntryService
import com.collederas.kroll.core.configentry.entries.ConfigType
import com.collederas.kroll.exceptions.ConfigValidationException
import com.collederas.kroll.support.factories.PersistedConfigEntryFactory
import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import com.collederas.kroll.support.factories.UserFactory
import com.collederas.kroll.user.AppUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant

@SpringBootTest
@Import(ConfigEntryHistoryListener::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class ConfigEntryHistoryIntegrationTests {
    @Autowired
    lateinit var appUserRepository: AppUserRepository

    @Autowired
    lateinit var historyRepo: ConfigEntryHistoryRepository

    @Autowired
    lateinit var configEntryService: ConfigEntryService

    @Autowired
    lateinit var persistedEnvironmentFactory: PersistedEnvironmentFactory

    @Autowired
    lateinit var envFactory: PersistedConfigEntryFactory

    @Test
    fun `creating config entry persists history`() {
        val user = UserFactory.create()
        appUserRepository.save(user)

        val env = persistedEnvironmentFactory.create(user)

        val dto =
            CreateConfigEntryDto(
                key = "testkey",
                type = ConfigType.BOOLEAN,
                value = "true",
                activeFrom = Instant.now(),
                activeUntil = Instant.now() + Duration.ofHours(1),
            )
        configEntryService.create(user.id, env.id, dto)

        val history = historyRepo.findAll()
        assertThat(history).hasSize(1)

        val historyEntry = history.first()
        assertEquals(historyEntry.environmentId, env.id)
        assertEquals(historyEntry.changeDescription, "Initial Creation")
    }

    @Test
    fun `updating config entry persists history`() {
        val user = UserFactory.create()
        appUserRepository.save(user)

        val env = persistedEnvironmentFactory.create(user)
        val entry =
            envFactory.create(
                environment = env,
                value = "true",
                createdBy = user.id,
            )

        val updateMsg = "Updated to false"
        val dto =
            UpdateConfigEntryDto(
                value = "false",
                changeDescription = updateMsg,
            )

        configEntryService.update(user.id, env.id, entry.configKey, dto)

        val history = historyRepo.findAll()
        assertThat(history).hasSize(1)

        val historyEntry = history.first()
        assertEquals(historyEntry.environmentId, env.id)
        assertEquals(historyEntry.changeDescription, updateMsg)
    }

    @Test
    fun `updating config entry with no effective changes throws exception and does not persist history`() {
        val user = UserFactory.create()
        appUserRepository.save(user)
        val env = persistedEnvironmentFactory.create(user)
        val entry =
            envFactory.create(
                environment = env,
                value = "true",
                createdBy = user.id,
            )

        val dto =
            UpdateConfigEntryDto(
                value = "true",
                changeDescription = "Some log message",
            )

        assertThrows<ConfigValidationException> {
            configEntryService.update(user.id, env.id, entry.configKey, dto)
        }

        assertThat(historyRepo.findAll()).isEmpty()
    }
}

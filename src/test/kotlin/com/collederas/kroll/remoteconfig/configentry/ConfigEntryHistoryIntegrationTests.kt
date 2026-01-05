package com.collederas.kroll.remoteconfig.configentry

import ConfigEntryHistoryListener
import com.collederas.kroll.core.configentry.ConfigEntryService
import com.collederas.kroll.core.configentry.ConfigType
import com.collederas.kroll.core.configentry.dto.CreateConfigEntryDto
import com.collederas.kroll.core.configentry.dto.UpdateConfigEntryDto
import com.collederas.kroll.core.configentry.history.ConfigEntryHistoryRepository
import com.collederas.kroll.support.factories.PersistedConfigEntryFactory
import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant
import java.util.*

@SpringBootTest
@Import(ConfigEntryHistoryListener::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class ConfigEntryHistoryIntegrationTests {
    @Autowired
    lateinit var historyRepo: ConfigEntryHistoryRepository

    @Autowired
    lateinit var configEntryService: ConfigEntryService

    @Autowired
    lateinit var persistedEnvironmentFactory: PersistedEnvironmentFactory

    @Autowired
    lateinit var persistedConfigEntryFactory: PersistedConfigEntryFactory

    @Test
    fun `creating config entry persists history`() {
        val env = persistedEnvironmentFactory.create()

        val dto = CreateConfigEntryDto(
            key = "testkey",
            type = ConfigType.BOOLEAN,
            value = "true",
            activeFrom = Instant.now(),
            activeUntil = Instant.now() + Duration.ofHours(1),
        )
        configEntryService.create(env.id, UUID.randomUUID(), dto)

        val history = historyRepo.findAll()
        assertThat(history).hasSize(1)

        val historyEntry = history.first()
        assertEquals(historyEntry.environmentId, env.id)
        assertEquals(historyEntry.changeDescription, "Initial Creation")
    }

    @Test
    fun `updating config entry persists history`() {
        val principalUserId = UUID.randomUUID()
        val env = persistedEnvironmentFactory.create()
        val entry = persistedConfigEntryFactory.create(
            environment = env,
            value = "true",
            createdBy = principalUserId,
        )

        val updateMsg = "Updated to false"
        val dto = UpdateConfigEntryDto(
            value = "false",
            changeDescription = updateMsg,
        )

        configEntryService.update(principalUserId, env.id, entry.configKey, dto)


        val history = historyRepo.findAll()
        assertThat(history).hasSize(1)

        val historyEntry = history.first()
        assertEquals(historyEntry.environmentId, env.id)
        assertEquals(historyEntry.changeDescription, updateMsg)
    }
}

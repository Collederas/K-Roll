package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.configentry.dto.CreateConfigEntryDto
import com.collederas.kroll.core.configentry.dto.UpdateConfigEntryDto
import com.collederas.kroll.core.environment.EnvironmentAccessGuard
import com.collederas.kroll.core.environment.EnvironmentRepository
import com.collederas.kroll.core.exceptions.ConfigEntryNotFoundException
import com.collederas.kroll.core.exceptions.ConfigValidationException
import com.collederas.kroll.core.exceptions.EnvironmentNotFoundException
import com.collederas.kroll.core.exceptions.InvalidConfigTypeException
import com.collederas.kroll.support.factories.ConfigEntryFactory
import com.collederas.kroll.support.factories.EnvironmentFactory
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class ConfigEntryServiceTests {
    private val environmentRepo = mockk<EnvironmentRepository>()
    private val configEntryRepo = mockk<ConfigEntryRepository>(relaxed = true)
    private val accessGuard = mockk<EnvironmentAccessGuard>()
    private val envId = UUID.randomUUID()

    private val configEntryService =
        ConfigEntryService(
            accessGuard,
            configEntryRepo,
            environmentRepo,
        )

    @Test
    fun `list throws when environment does not exist`() {
        every { environmentRepo.existsById(any()) } returns false
        every {
            accessGuard.requireOwner(any(), any())
        } just Runs

        assertThrows<EnvironmentNotFoundException> {
            configEntryService.list(UUID.randomUUID(), envId)
        }
    }

    @Test
    fun `list returns entries for environment`() {
        val entity = ConfigEntryFactory.create()

        every { environmentRepo.existsById(envId) } returns true
        every { configEntryRepo.findAllByEnvironmentId(envId) } returns listOf(entity)
        every {
            accessGuard.requireOwner(any(), any())
        } just Runs

        val result = configEntryService.list(UUID.randomUUID(), envId)

        assertEquals(1, result.size)
    }

    @Test
    fun `fetchEffectiveConfig throws when environment does not exist`() {
        every { environmentRepo.existsById(envId) } returns false
        every {
            accessGuard.requireOwner(any(), any())
        } just Runs

        assertThrows<EnvironmentNotFoundException> {
            configEntryService.fetchEffectiveConfig(envId)
        }
    }

    @Test
    fun `fetchEffectiveConfig returns parsed active values`() {
        val entity =
            ConfigEntryFactory.create(
                key = "flag",
                value = "true",
                type = ConfigType.BOOLEAN,
            )

        val now = Instant.now()
        val fixedClock = Clock.fixed(now, ZoneOffset.UTC)
        val service =
            ConfigEntryService(
                accessGuard,
                configEntryRepo,
                environmentRepo,
                fixedClock,
            )

        every { environmentRepo.existsById(entity.environment.id) } returns true
        every { configEntryRepo.findActiveConfigs(entity.environment.id, now) } returns listOf(entity)

        val result = service.fetchEffectiveConfig(entity.environment.id)

        assertEquals(true, result["flag"])
    }

    @Test
    fun `create rejects invalid json`() {
        val environment = EnvironmentFactory.create()
        every { environmentRepo.findByIdOrNull(envId) } returns environment

        val dto =
            CreateConfigEntryDto(
                key = "cfg",
                value = "{invalid",
                type = ConfigType.JSON,
            )
        every {
            accessGuard.requireOwner(any(), any())
        } just Runs

        assertThrows<ConfigValidationException> {
            configEntryService.create(UUID.randomUUID(), envId, dto)
        }
    }

    @Test
    fun `create saves config entry`() {
        val environment = EnvironmentFactory.create()
        val objectMapper = ObjectMapper()
        every { environmentRepo.findByIdOrNull(envId) } returns environment
        every { configEntryRepo.save(any()) } answers { firstArg() }
        every {
            accessGuard.requireOwner(any(), any())
        } just Runs

        val dto =
            CreateConfigEntryDto(
                key = "cfg",
                value = objectMapper.writeValueAsString(mapOf("key" to "value")),
                type = ConfigType.JSON,
            )
        val result = configEntryService.create(UUID.randomUUID(), envId, dto)

        assertEquals(dto.key, result.key)
    }

    @Test
    fun `update throws when config does not exist`() {
        every {
            configEntryRepo.findByEnvironmentIdAndConfigKey(envId, "key")
        } returns null

        val dto =
            UpdateConfigEntryDto(
                value = "value",
                type = ConfigType.STRING,
            )
        every {
            accessGuard.requireOwner(any(), any())
        } just Runs

        assertThrows<ConfigEntryNotFoundException> {
            configEntryService.update(UUID.randomUUID(), envId, "key", dto)
        }
    }

    @Test
    fun `update merges partial dto`() {
        val entity = ConfigEntryFactory.create(value = "old", type = ConfigType.STRING)

        every { configEntryRepo.findByEnvironmentIdAndConfigKey(envId, "key") } returns entity
        every { configEntryRepo.save(any()) } answers { firstArg() }

        val dto =
            UpdateConfigEntryDto(
                value = "new",
                type = null,
            )

        every {
            accessGuard.requireOwner(any(), any())
        } just Runs

        val result = configEntryService.update(UUID.randomUUID(), envId, "key", dto)

        assertEquals("new", result.value)
        assertEquals(ConfigType.STRING, result.type)
    }

    @Test
    fun `delete removes existing config`() {
        val entity = ConfigEntryFactory.create(key = "key", type = ConfigType.STRING)

        every { configEntryRepo.findByEnvironmentIdAndConfigKey(envId, entity.configKey) } returns entity
        every { configEntryRepo.delete(entity) } just Runs
        every {
            accessGuard.requireOwner(any(), any())
        } just Runs

        configEntryService.delete(UUID.randomUUID(), envId, "key")

        verify { configEntryRepo.delete(entity) }
    }
}

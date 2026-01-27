package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.configentry.dto.ConfigEntryResponseDto
import com.collederas.kroll.core.configentry.dto.CreateConfigEntryDto
import com.collederas.kroll.core.configentry.dto.UpdateConfigEntryDto
import com.collederas.kroll.core.configentry.history.ConfigEntrySnapshot
import com.collederas.kroll.core.environment.EnvironmentAccessGuard
import com.collederas.kroll.core.environment.EnvironmentRepository
import com.collederas.kroll.exceptions.ConfigEntryNotFoundException
import com.collederas.kroll.exceptions.ConfigValidationException
import com.collederas.kroll.exceptions.EnvironmentNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.*

@Service
class ConfigEntryService(
    private val envAccessGuard: EnvironmentAccessGuard,
    private val configEntryRepository: ConfigEntryRepository,
    private val environmentRepository: EnvironmentRepository,
    private val clock: Clock = Clock.systemUTC(),
    // JavaTimeModule allows converting a ConfigEntrySnapshot
    // (which contains Instant fields like activeUntil) into a JSON string
    private val objectMapper: ObjectMapper =
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
) {
    @Transactional(readOnly = true)
    fun list(
        userId: UUID,
        envId: UUID,
    ): List<ConfigEntryResponseDto> {
        envAccessGuard.requireOwner(envId, userId)

        if (!environmentRepository.existsById(envId)) {
            throw EnvironmentNotFoundException("Environment with ID $envId not found")
        }
        return configEntryRepository.findAllByEnvironmentId(envId).map { it.toResponseDto() }
    }

    @Transactional(readOnly = true)
    fun fetchEffectiveConfig(envId: UUID): Map<String, Any> {
        if (!environmentRepository.existsById(envId)) {
            throw EnvironmentNotFoundException("Environment with ID $envId not found")
        }
        val now = Instant.now(clock)
        val entities = configEntryRepository.findActiveConfigs(envId, now)

        // TODO: Dto?
        return entities.associate { entity ->
            entity.configKey to parseValue(entity.configValue, entity.configType)
        }
    }

    @Transactional
    fun create(
        userId: UUID,
        envId: UUID,
        dto: CreateConfigEntryDto,
    ): ConfigEntryResponseDto {
        envAccessGuard.requireOwner(envId, userId)

        val environment =
            environmentRepository.findByIdOrNull(envId)
                ?: throw EnvironmentNotFoundException("Environment with ID $envId not found")

        if (configEntryRepository.existsByEnvironmentIdAndConfigKey(envId, dto.key)) {
            throw ConfigValidationException(
                listOf("Config entry with key '${dto.key}' already exists in environment $envId"),
            )
        }

        val snapshotDto =
            ConfigEntrySnapshot(
                key = dto.key,
                value = dto.value,
                type = dto.type,
                activeFrom = dto.activeFrom,
                activeUntil = dto.activeUntil,
                environmentId = envId,
                changeDescription = "Initial Creation",
                changedBy = userId,
            )
        val snapshotJson = objectMapper.writeValueAsString(snapshotDto)

        val entity =
            ConfigEntryEntity.create(
                environment = environment,
                key = dto.key,
                value = dto.value,
                type = dto.type,
                activeFrom = dto.activeFrom,
                activeUntil = dto.activeUntil,
                createdBy = userId,
                snapshotJson = snapshotJson,
            )

        return configEntryRepository.save(entity).toResponseDto()
    }

    @Transactional
    fun update(
        userId: UUID,
        envId: UUID,
        key: String,
        dto: UpdateConfigEntryDto,
    ): ConfigEntryResponseDto {
        envAccessGuard.requireOwner(envId, userId)

        val entity =
            configEntryRepository.findByEnvironmentIdAndConfigKey(envId, key)
                ?: throw ConfigEntryNotFoundException("Config '$key' not found in env $envId")

        val snapshotDto =
            ConfigEntrySnapshot(
                key = key,
                value = dto.value ?: entity.configValue,
                type = dto.type ?: entity.configType,
                activeFrom = dto.activeFrom ?: entity.activeFrom,
                activeUntil = dto.activeUntil ?: entity.activeUntil,
                changeDescription = dto.changeDescription,
                changedBy = userId,
                environmentId = envId,
            )
        val snapshotJson = objectMapper.writeValueAsString(snapshotDto)

        entity.update(
            editorId = userId,
            newValue = dto.value,
            newType = dto.type,
            newActiveFrom = dto.activeFrom,
            clearActiveFrom = dto.clearActiveFrom,
            newActiveUntil = dto.activeUntil,
            clearActiveUntil = dto.clearActiveUntil,
            changeDescription = dto.changeDescription,
            snapshotJson = snapshotJson,
        )

        return configEntryRepository.save(entity).toResponseDto()
    }

    @Transactional
    fun delete(
        userId: UUID,
        envId: UUID,
        key: String,
    ) {
        envAccessGuard.requireOwner(envId, userId)

        val entity =
            configEntryRepository.findByEnvironmentIdAndConfigKey(envId, key)
                ?: throw ConfigEntryNotFoundException(
                    "Config with key '$key' " +
                        "not found in environment $envId",
                )
        configEntryRepository.delete(entity)
    }

    private fun ConfigEntryEntity.toResponseDto() =
        ConfigEntryResponseDto(
            key = configKey,
            value = configValue,
            type = configType,
            activeFrom = activeFrom,
            activeUntil = activeUntil,
            environmentId = environment.id,
        )

    private fun parseValue(
        value: String,
        type: ConfigType,
    ): Any =
        when (type) {
            ConfigType.BOOLEAN -> value.toBoolean()
            ConfigType.NUMBER -> value.toBigDecimal()
            ConfigType.STRING -> value
            ConfigType.JSON -> objectMapper.readTree(value)
        }
}

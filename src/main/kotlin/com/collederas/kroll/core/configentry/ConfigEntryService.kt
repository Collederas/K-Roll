package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.configentry.dto.ConfigEntryResponseDto
import com.collederas.kroll.core.configentry.dto.CreateConfigEntryDto
import com.collederas.kroll.core.configentry.dto.UpdateConfigEntryDto
import com.collederas.kroll.core.configentry.history.ConfigEntrySnapshot
import com.collederas.kroll.core.environment.EnvironmentRepository
import com.collederas.kroll.core.exceptions.ConfigEntryNotFoundException
import com.collederas.kroll.core.exceptions.EnvironmentNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.*

@Service
class ConfigEntryService(
    private val configEntryRepository: ConfigEntryRepository,
    private val environmentRepository: EnvironmentRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val objectMapper: ObjectMapper = ObjectMapper(),

    ) {
    fun list(envId: UUID): List<ConfigEntryResponseDto> {
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

        return entities.associate { entity ->
            entity.configKey to parseValue(entity.configValue, entity.configType)
        }
    }

    @Transactional
    fun create(
        envId: UUID,
        createdBy: UUID,
        dto: CreateConfigEntryDto,
    ): ConfigEntryResponseDto {
        val environment =
            environmentRepository.findByIdOrNull(envId)
                ?: throw EnvironmentNotFoundException("Environment with ID $envId not found")

        if (dto.type == ConfigType.JSON) {
            validateJson(dto.value)
        }

        val snapshotDto = ConfigEntrySnapshot(
            key = dto.key,
            value = dto.value,
            type = dto.type,
            activeFrom = dto.activeFrom,
            activeUntil = dto.activeUntil,
            environmentId = envId,
            changeDescription = "Initial Creation",
            changedBy = createdBy,
        )
        val snapshotJson = objectMapper.writeValueAsString(snapshotDto)

        val entity = ConfigEntryEntity.create(
            environment = environment,
            createDto = dto,
            createdBy = createdBy,
            snapshotJson = snapshotJson
        )

        return configEntryRepository.save(entity).toResponseDto()
    }

    @Transactional
    fun update(
        editorUserId: UUID,
        envId: UUID,
        key: String,
        dto: UpdateConfigEntryDto,
    ): ConfigEntryResponseDto {
        val entity = configEntryRepository.findByEnvironmentIdAndConfigKey(envId, key)
            ?: throw ConfigEntryNotFoundException("Config '$key' not found in env $envId")

        val snapshotDto = ConfigEntrySnapshot(
            key = key,
            value = dto.value ?: entity.configValue,
            type = dto.type ?: entity.configType,
            activeFrom = dto.activeFrom ?: entity.activeFrom,
            activeUntil = dto.activeUntil ?: entity.activeUntil,
            changeDescription = dto.changeDescription,
            changedBy = editorUserId,
            environmentId = envId
        )
        val snapshotJson = objectMapper.writeValueAsString(snapshotDto)

        entity.update(editorUserId, dto, snapshotJson)

        return configEntryRepository.save(entity).toResponseDto()
    }

    @Transactional
    fun delete(
        envId: UUID,
        key: String,
    ) {
        val entity =
            configEntryRepository.findByEnvironmentIdAndConfigKey(envId, key)
                ?: throw ConfigEntryNotFoundException(
                    "Config with key '$key' " +
                        "not found in environment $envId"
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

    private fun parseValue(value: String, type: ConfigType): Any {
        return when (type) {
            ConfigType.BOOLEAN -> value.toBoolean()
            ConfigType.NUMBER -> value.toBigDecimal()
            ConfigType.STRING -> value
            ConfigType.JSON -> objectMapper.readTree(value)
        }
    }

    private fun validateJson(value: String) {
        try {
            objectMapper.readTree(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON format")
        }
    }
}

package com.collederas.kroll.core.configentry.entries

import com.collederas.kroll.core.configentry.ConfigEntryResponseDto
import com.collederas.kroll.core.configentry.CreateConfigEntryDto
import com.collederas.kroll.core.configentry.UpdateConfigEntryDto
import com.collederas.kroll.core.configentry.diff.ConfigDiffCalculator
import com.collederas.kroll.core.configentry.diff.SemanticDiff
import com.collederas.kroll.core.configentry.audit.ConfigEntrySnapshot
import com.collederas.kroll.core.configentry.validation.ConfigEntryValidator
import com.collederas.kroll.core.environment.EnvironmentAuthorizationService
import com.collederas.kroll.core.environment.EnvironmentRepository
import com.collederas.kroll.exceptions.ConfigEntryNotFoundException
import com.collederas.kroll.exceptions.ConfigValidationException
import com.collederas.kroll.exceptions.EnvironmentNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.*

@Service
class ConfigEntryService(
    private val envAccessGuard: EnvironmentAuthorizationService,
    private val configValueValidator: ConfigEntryValidator,
    private val configDiffCalculator: ConfigDiffCalculator,
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
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun list(
        userId: UUID,
        envId: UUID,
    ): List<ConfigEntryResponseDto> {
        if (!environmentRepository.existsById(envId)) {
            throw EnvironmentNotFoundException("Environment with ID $envId not found")
        }
        return configEntryRepository.findAllByEnvironmentId(envId).map { it.toResponseDto() }
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun create(
        userId: UUID,
        envId: UUID,
        dto: CreateConfigEntryDto,
    ): ConfigEntryResponseDto {
        val environment =
            environmentRepository.findByIdOrNull(envId)
                ?: throw EnvironmentNotFoundException("Environment with ID $envId not found")

        if (configEntryRepository.existsByEnvironmentIdAndConfigKey(envId, dto.key)) {
            throw ConfigValidationException(
                listOf("Config entry with key '${dto.key}' already exists in environment $envId"),
            )
        }

        // this must throw if validation fails
        validateConfigEntry(
            value = dto.value,
            type = dto.type,
            activeFrom = dto.activeFrom,
            activeUntil = dto.activeUntil
        )

        val entity = ConfigEntryEntity(
            environment = environment,
            configKey = dto.key,
            configValue = dto.value,
            configType = dto.type,
            activeFrom = dto.activeFrom,
            activeUntil = dto.activeUntil,
            createdBy = userId,
        )

        // cheese!
        val snapshotJson = createSnapshot(
            key = dto.key,
            value = dto.value,
            type = dto.type,
            activeFrom = dto.activeFrom,
            activeUntil = dto.activeUntil,
            environmentId = envId,
            changedBy = userId,
            changeDescription = "Initial Creation"
        )
        entity.recordCreateSnapshot(userId, snapshotJson)

        return configEntryRepository.save(entity).toResponseDto()
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun update(
        userId: UUID,
        envId: UUID,
        key: String,
        dto: UpdateConfigEntryDto,
    ): ConfigEntryResponseDto {
        val entity =
            configEntryRepository.findByEnvironmentIdAndConfigKey(envId, key)
                ?: throw ConfigEntryNotFoundException("Config '$key' not found in env $envId")

        if (!wouldEntityChange(entity, dto)) throw ConfigValidationException(listOf("Update rejected: new entry is identical to current."))

        val targetValue = dto.value ?: entity.configValue
        val targetType = dto.type ?: entity.configType
        val targetActiveFrom = if (dto.clearActiveFrom) null else (dto.activeFrom ?: entity.activeFrom)
        val targetActiveUntil = if (dto.clearActiveUntil) null else (dto.activeUntil ?: entity.activeUntil)

        // this must throw if validation fails
        validateConfigEntry(
            value = targetValue,
            type = targetType,
            activeFrom = targetActiveFrom,
            activeUntil = targetActiveUntil
        )

        // update dto for snapshot creation
        // TODO: this can be done better/elsewhere
        dto.value?.let { entity.configValue = it }
        dto.type?.let { entity.configType = it }

        if (dto.clearActiveFrom) {
            entity.activeFrom = null
        } else {
            dto.activeFrom?.let { entity.activeFrom = it }
        }

        if (dto.clearActiveUntil) {
            entity.activeUntil = null
        } else {
            dto.activeUntil?.let { entity.activeUntil = it }
        }

        // cheese!
        val snapshotJson = createSnapshot(
            key = key,
            value = targetValue,
            type = targetType,
            activeFrom = targetActiveFrom,
            activeUntil = targetActiveUntil,
            environmentId = envId,
            changedBy = userId,
            changeDescription = dto.changeDescription
        )

        entity.recordUpdateSnapshot(
            editorId = userId,
            changeDescription = dto.changeDescription,
            snapshotJson = snapshotJson,
        )

        return configEntryRepository.save(entity).toResponseDto()
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun delete(
        userId: UUID,
        envId: UUID,
        key: String,
    ) {
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

    private fun validateConfigEntry(
        value: String,
        type: ConfigType,
        activeFrom: Instant?,
        activeUntil: Instant?
    ) {
        val errors = mutableListOf<String>()

        if (activeFrom != null && activeUntil != null && activeUntil.isBefore(activeFrom)) {
            errors.add("Active Until cannot be before Active From")
        }

        errors.addAll(configValueValidator.validate(value, type))

        if (errors.isNotEmpty()) {
            throw ConfigValidationException(errors)
        }
    }

    private fun wouldEntityChange(
        entity: ConfigEntryEntity,
        dto: UpdateConfigEntryDto
    ): Boolean {
        val targetValue = dto.value ?: entity.configValue
        val targetType = dto.type ?: entity.configType

        data class Metadata(
            val type: ConfigType,
            val activeFrom: Instant?,
            val activeUntil: Instant?,
        )

        val targetMetadata =
            Metadata(
                type = targetType,
                activeFrom = if (dto.clearActiveFrom) null else dto.activeFrom ?: entity.activeFrom,
                activeUntil = if (dto.clearActiveUntil) null else (dto.activeUntil ?: entity.activeUntil),
            )

        val currentMetadata = Metadata(entity.configType, entity.activeFrom, entity.activeUntil)
        if (targetMetadata != currentMetadata) {
            return true
        }

        // only json requires semantic diff, the rest are string equality checks
        if (targetType == ConfigType.JSON) {
            val diff = configDiffCalculator.jsonSemanticDiff(entity.configValue, targetValue)
            return when (diff) {
                SemanticDiff.Same -> false
                SemanticDiff.Different -> true
                is SemanticDiff.Invalid -> throw ConfigValidationException(listOf("New value is invalid"))
            }
        }

        // log? throw? why are we here?
        return false
    }

    private fun createSnapshot(
        key: String,
        value: String,
        type: ConfigType,
        activeFrom: Instant?,
        activeUntil: Instant?,
        environmentId: UUID,
        changedBy: UUID,
        changeDescription: String?
    ): String {
        val snapshot = ConfigEntrySnapshot(
            key = key,
            value = value,
            type = type,
            activeFrom = activeFrom,
            activeUntil = activeUntil,
            environmentId = environmentId,
            changedBy = changedBy,
            changeDescription = changeDescription
        )
        return objectMapper.writeValueAsString(snapshot)
    }
}

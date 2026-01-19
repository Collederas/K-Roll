package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.environment.EnvironmentEntity
import com.collederas.kroll.core.exceptions.ConfigValidationException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.*
import org.springframework.data.domain.AbstractAggregateRoot
import java.time.Instant
import java.util.*

enum class ConfigType {
    BOOLEAN,
    STRING,
    NUMBER,
    JSON,
}

@Entity
@Table(
    name = "config_entries",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["environment_id", "config_key"]),
    ],
    indexes = [
        Index(name = "idx_config_entry_key", columnList = "config_key"),
    ],
)
class ConfigEntryEntity(
    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    val environment: EnvironmentEntity,
    @Column(name = "created_by")
    var createdBy: UUID? = null,
    @Column(name = "config_key", nullable = false)
    var configKey: String,
    @Column(name = "config_value", nullable = false)
    var configValue: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "config_type", nullable = false)
    var configType: ConfigType,
    @Column(name = "active_from")
    var activeFrom: Instant? = null,
    @Column(name = "active_until")
    var activeUntil: Instant? = null,
) : AbstractAggregateRoot<ConfigEntryEntity>() {
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }

    companion object {
        private val STRICT_JSON_MAPPER: ObjectMapper =
            ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)

        fun create(
            environment: EnvironmentEntity,
            key: String,
            value: String,
            type: ConfigType,
            activeFrom: Instant?,
            activeUntil: Instant?,
            createdBy: UUID,
            snapshotJson: String,
        ): ConfigEntryEntity {
            val entity =
                ConfigEntryEntity(
                    environment = environment,
                    configKey = key,
                    configValue = value,
                    configType = type,
                    activeFrom = activeFrom,
                    activeUntil = activeUntil,
                    createdBy = createdBy,
                )

            entity.validateState()

            entity.registerEvent(
                ConfigEntryUpdatedEvent(
                    configEntryId = entity.id,
                    environmentId = environment.id,
                    changedBy = createdBy,
                    changeDescription = "Initial Creation",
                    snapshot = snapshotJson,
                ),
            )

            return entity
        }
    }

    fun update(
        editorId: UUID,
        newValue: String?,
        newType: ConfigType?,
        newActiveFrom: Instant?,
        clearActiveFrom: Boolean,
        newActiveUntil: Instant?,
        clearActiveUntil: Boolean,
        changeDescription: String?,
        snapshotJson: String,
    ): ConfigEntryEntity {
        newValue?.let { this.configValue = it }
        newType?.let { this.configType = it }

        if (clearActiveFrom) {
            this.activeFrom = null
        } else {
            newActiveFrom?.let { this.activeFrom = it }
        }

        if (clearActiveUntil) {
            this.activeUntil = null
        } else {
            newActiveUntil?.let { this.activeUntil = it }
        }

        validateState()

        this.registerEvent(
            ConfigEntryUpdatedEvent(
                configEntryId = this.id,
                environmentId = this.environment.id,
                changedBy = editorId,
                changeDescription = changeDescription,
                snapshot = snapshotJson,
            ),
        )

        return this
    }

    fun validateState() {
        val errors = mutableListOf<String>()

        if (activeFrom != null && activeUntil != null && activeUntil!!.isBefore(activeFrom)) {
            errors.add("Active Until date ($activeUntil) cannot be before Active From date ($activeFrom)")
        }

        when (configType) {
            ConfigType.NUMBER -> {
                if (configValue.toBigDecimalOrNull() == null) {
                    errors.add("Value '$configValue' is not a valid NUMBER")
                }
            }

            ConfigType.BOOLEAN -> {
                if (
                    !configValue.equals("true", ignoreCase = true) &&
                    !configValue.equals("false", ignoreCase = true)
                ) {
                    errors.add("Value '$configValue' is not a valid BOOLEAN")
                }
            }

            ConfigType.STRING -> {
                if (configValue.isBlank()) {
                    errors.add("Config value cannot be empty for STRING type")
                }
            }

            ConfigType.JSON -> {
                validateJsonConfig(configValue, errors)
            }
        }

        if (errors.isNotEmpty()) {
            throw ConfigValidationException(errors)
        }
    }

    private fun validateJsonConfig(
        value: String,
        errors: MutableList<String>,
    ) {
        if (value.isBlank()) {
            errors.add("JSON value cannot be blank")
            return
        }

        val node =
            try {
                STRICT_JSON_MAPPER.readTree(value)
            } catch (_: JsonProcessingException) {
                errors.add("Value is not valid JSON")
                return
            }

        if (!node.isObject && !node.isArray) {
            errors.add("JSON value must be a JSON object or array")
        }

        val maxBytes = 64 * 1024
        if (value.toByteArray(Charsets.UTF_8).size > maxBytes) {
            errors.add("JSON value exceeds maximum size of 64KB")
        }
    }
}

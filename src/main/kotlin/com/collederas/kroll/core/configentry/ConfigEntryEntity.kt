package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.environment.EnvironmentEntity
import com.collederas.kroll.exceptions.ConfigValidationException
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

sealed interface SemanticDiff {
    object Same : SemanticDiff

    object Different : SemanticDiff

    data class Invalid(
        val cause: Exception,
    ) : SemanticDiff
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
        const val MAX_JSON_BYTES: Int = 64 * 1024

        fun compareJson(
            val1: String,
            val2: String,
        ): SemanticDiff =
            try {
                val node1 = STRICT_JSON_MAPPER.readTree(val1)
                val node2 = STRICT_JSON_MAPPER.readTree(val2)

                if (node1 == node2) {
                    SemanticDiff.Same
                } else {
                    SemanticDiff.Different
                }
            } catch (e: JsonProcessingException) {
                SemanticDiff.Invalid(e)
            }

        fun areValuesSemanticallyDifferent(
            type: ConfigType,
            val1: String,
            val2: String,
        ): SemanticDiff {
            if (val1 == val2) return SemanticDiff.Same

            return when (type) {
                ConfigType.STRING ->
                    SemanticDiff.Different

                ConfigType.BOOLEAN ->
                    if (val1.equals(val2, ignoreCase = true)) {
                        SemanticDiff.Same
                    } else {
                        SemanticDiff.Different
                    }

                ConfigType.NUMBER ->
                    try {
                        val n1 = val1.toBigDecimal()
                        val n2 = val2.toBigDecimal()
                        if (n1.compareTo(n2) == 0) {
                            SemanticDiff.Same
                        } else {
                            SemanticDiff.Different
                        }
                    } catch (e: NumberFormatException) {
                        SemanticDiff.Invalid(e)
                    }

                ConfigType.JSON ->
                    compareJson(val1, val2)
            }
        }

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
        if (
            !wouldChange(
                newValue,
                newType,
                newActiveFrom,
                clearActiveFrom,
                newActiveUntil,
                clearActiveUntil,
            )
        ) {
            throw ConfigValidationException(
                listOf("Update rejected: no effective changes detected"),
            )
        }

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
        var isValid = true

        if (value.isBlank()) {
            errors.add("JSON value cannot be blank")
            isValid = false
        }

        if (value.toByteArray(Charsets.UTF_8).size > MAX_JSON_BYTES) {
            errors.add("JSON value exceeds maximum size of 64KB")
            isValid = false
        }

        if (isValid) {
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
        }
    }

    fun wouldChange(
        newValue: String?,
        newType: ConfigType?,
        newActiveFrom: Instant?,
        clearActiveFrom: Boolean,
        newActiveUntil: Instant?,
        clearActiveUntil: Boolean,
    ): Boolean {
        data class Metadata(
            val type: ConfigType,
            val activeFrom: Instant?,
            val activeUntil: Instant?,
        )

        val targetMetadata =
            Metadata(
                type = newType ?: configType,
                activeFrom = if (clearActiveFrom) null else (newActiveFrom ?: activeFrom),
                activeUntil = if (clearActiveUntil) null else (newActiveUntil ?: activeUntil),
            )

        val currentMetadata = Metadata(configType, activeFrom, activeUntil)
        if (targetMetadata != currentMetadata) {
            return true
        }

        val targetValue = newValue ?: configValue

        return when (
            val diff =
                areValuesSemanticallyDifferent(
                    targetMetadata.type,
                    configValue,
                    targetValue,
                )
        ) {
            SemanticDiff.Same -> false
            SemanticDiff.Different -> true
            is SemanticDiff.Invalid ->
                throw ConfigValidationException(
                    listOf(
                        "Invalid ${targetMetadata.type} value: ${diff.cause.message}",
                    ),
                )
        }
    }
}

package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.environment.EnvironmentEntity
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
        // Date sanity
        if (activeFrom != null && activeUntil != null) {
            require(!activeUntil!!.isBefore(activeFrom)) {
                "activeUntil ($activeUntil) cannot be before activeFrom ($activeFrom)"
            }
        }

        // Type safety
        when (configType) {
            ConfigType.NUMBER ->
                require(configValue.toBigDecimalOrNull() != null) {
                    "Value '$configValue' is not a valid NUMBER"
                }

            ConfigType.BOOLEAN ->
                require(
                    configValue.equals("true", ignoreCase = true) ||
                        configValue.equals("false", ignoreCase = true),
                ) {
                    "Value '$configValue' is not a valid BOOLEAN"
                }

            ConfigType.STRING ->
                require(configValue.isNotBlank()) {
                    "Config value cannot be empty"
                }

            ConfigType.JSON -> {
                // JSON validity enforced at service layer
            }
        }
    }
}

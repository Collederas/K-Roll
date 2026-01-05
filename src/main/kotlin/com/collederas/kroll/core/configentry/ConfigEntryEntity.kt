package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.configentry.dto.CreateConfigEntryDto
import com.collederas.kroll.core.configentry.dto.UpdateConfigEntryDto
import com.collederas.kroll.core.environment.EnvironmentEntity
import jakarta.persistence.*
import org.springframework.data.domain.AbstractAggregateRoot
import java.time.Instant
import java.util.*

enum class ConfigType {
    BOOLEAN, STRING, NUMBER, JSON
}

@Entity
@Table(
    name = "config_entries",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["environment_id", "config_key"])
    ],
    indexes = [
        Index(name = "idx_config_entry_key", columnList = "config_key")
    ]
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
            createDto: CreateConfigEntryDto,
            createdBy: UUID,
            environment: EnvironmentEntity,
            snapshotJson: String
        ): ConfigEntryEntity {
            val entity = ConfigEntryEntity(
                environment = environment,
                configKey = createDto.key,
                configValue = createDto.value,
                configType = createDto.type,
                activeFrom = createDto.activeFrom,
                activeUntil = createDto.activeUntil,
                createdBy = createdBy,
            )
            entity.validateState()

            entity.registerEvent(
                ConfigEntryUpdatedEvent(
                    configEntryId = entity.id,
                    environmentId = environment.id,
                    changedBy = createdBy,
                    changeDescription = "Initial Creation",
                    snapshot = snapshotJson
                )
            )

            return entity
        }
    }

    fun update(
        editorId: UUID,
        updateDto: UpdateConfigEntryDto,
        snapshotJson: String
    ): ConfigEntryEntity {
        updateDto.value?.let { this.configValue = it }
        updateDto.type?.let { this.configType = it }

        if (updateDto.clearActiveFrom) this.activeFrom = null
        else updateDto.activeFrom?.let { this.activeFrom = it }

        if (updateDto.clearActiveUntil) this.activeUntil = null
        else updateDto.activeUntil?.let { this.activeUntil = it }

        validateState()

        this.registerEvent(
            ConfigEntryUpdatedEvent(
                configEntryId = this.id,
                environmentId = this.environment.id,
                changedBy = editorId,
                changeDescription = updateDto.changeDescription,
                snapshot = snapshotJson
            )
        )
        return this
    }

    fun validateState() {
        // Date sanity
        if (activeFrom != null && activeUntil != null) {
            if (activeUntil!!.isBefore(activeFrom)) {
                throw IllegalArgumentException("activeUntil ($activeUntil) cannot be before activeFrom ($activeFrom)")
            }
        }

        // Type safety
        when (configType) {
            ConfigType.NUMBER -> {
                if (configValue.toBigDecimalOrNull() == null) {
                    throw IllegalArgumentException("Value '$configValue' is not a valid NUMBER")
                }
            }

            ConfigType.BOOLEAN -> {
                if (!configValue.equals("true", ignoreCase = true) &&
                    !configValue.equals("false", ignoreCase = true)
                ) {
                    throw IllegalArgumentException("Value '$configValue' is not a valid BOOLEAN")
                }
            }

            ConfigType.STRING -> {
                if (configValue.isBlank()) {
                    throw IllegalArgumentException("Config value cannot be empty")
                }
            }

            else -> {

            }
        }
    }
}

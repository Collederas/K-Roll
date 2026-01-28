package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.configentry.audit.ConfigEntryUpdatedEvent
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

    fun recordCreateSnapshot(
        createdBy: UUID,
        snapshotJson: String,
    ) {
        this.registerEvent(
            ConfigEntryUpdatedEvent(
                configEntryId = this.id,
                environmentId = this.environment.id,
                changedBy = createdBy,
                changeDescription = "Initial Creation",
                snapshot = snapshotJson,
            ),
        )
    }


    fun recordUpdateSnapshot(
        editorId: UUID,
        changeDescription: String?,
        snapshotJson: String,
    ): ConfigEntryEntity {

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
}

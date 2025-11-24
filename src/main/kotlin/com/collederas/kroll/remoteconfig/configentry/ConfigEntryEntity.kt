package com.collederas.kroll.remoteconfig.configentry

import com.collederas.kroll.remoteconfig.environment.EnvironmentEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "config_entries",
)
class ConfigEntryEntity(
    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    val environment: EnvironmentEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    val collection: ConfigCollectionEntity,
    @Column(name = "config_key", nullable = false)
    val configKey: String,
    @Column(name = "config_value", nullable = false)
    val configValue: String,
    @Column(name = "config_type", nullable = false)
    val configType: String,
    @Column(name = "active_from")
    val activeFrom: Instant? = null,
    @Column(name = "active_until")
    val activeUntil: Instant? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)

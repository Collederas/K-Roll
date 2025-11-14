package com.collederas.kroll.remoteconfig.configentry

import com.collederas.kroll.remoteconfig.environment.EnvironmentEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "config_collections")
class ConfigCollectionEntity(
    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    val environment: EnvironmentEntity,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant
)
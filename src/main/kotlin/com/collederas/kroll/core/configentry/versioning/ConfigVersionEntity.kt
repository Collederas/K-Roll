package com.collederas.kroll.core.configentry.versioning

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "config_versions",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["environment_id", "version"])
    ],
    indexes = [
        Index(name = "idx_config_versions_environment", columnList = "environment_id"),
        Index(name = "idx_config_versions_created_at", columnList = "created_at DESC"),
    ]
)
class ConfigVersionEntity(

    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "environment_id", nullable = false)
    val environmentId: UUID,

    @Column(nullable = false)
    val version: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "created_by")
    val createdBy: UUID? = null,

    @Column(name = "contract_hash", nullable = false)
    val contractHash: String,

    @Column
    val notes: String? = null,
)

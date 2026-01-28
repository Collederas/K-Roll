package com.collederas.kroll.core.configentry.versioning.snapshot

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "config_snapshots",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["environment_id", "version"])
    ],
    indexes = [
        Index(name = "idx_config_snapshots_env_version", columnList = "environment_id, version"),
    ]
)
class ConfigSnapshotEntity(

    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "environment_id", nullable = false)
    val environmentId: UUID,

    @Column(nullable = false)
    val version: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "contract_hash", nullable = false)
    val contractHash: String,

    // tobe JSON one day
    @Column(name = "snapshot_json", nullable = false)
    val snapshotJson: String,
)

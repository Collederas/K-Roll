package com.collederas.kroll.core.configentry.versioning.snapshot

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "config_snapshots")
class ConfigSnapshotEntity(

    @Id
    @Column(name = "version_id", nullable = false)
    val versionId: UUID,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    // JSON stored as TEXT for MVP
    @Column(name = "snapshot_json", nullable = false)
    val snapshotJson: String,

    @Column(name = "diff_payload")
    val diffPayload: String? = null,
)

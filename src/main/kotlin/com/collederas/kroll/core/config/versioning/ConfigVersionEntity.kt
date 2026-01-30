package com.collederas.kroll.core.config.versioning

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "config_versions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_config_versions_env_version",
            columnNames = ["environment_id", "version_sequence"],
        ),
        UniqueConstraint(
            name = "uq_config_versions_env_label",
            columnNames = ["environment_id", "version_label"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_config_versions_env_seq_desc",
            columnList = "environment_id, version_sequence DESC",
        ),
        Index(
            name = "idx_config_versions_created_at",
            columnList = "created_at DESC",
        ),
    ],
)
class ConfigVersionEntity(
    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "environment_id", nullable = false)
    val environmentId: UUID,
    // for machines
    @Column(name = "version_sequence", nullable = false)
    val versionSequence: Long,
    // for humans
    @Column(name = "version_label", nullable = false)
    val versionLabel: String,
    // Integrity
    @Column(name = "contract_hash", nullable = false)
    val contractHash: String,
    @Column(name = "parent_hash")
    val parentHash: String? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "created_by")
    val createdBy: UUID? = null,
    @Column(name = "change_log")
    val changeLog: String? = null,
)

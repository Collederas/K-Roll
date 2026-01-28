package com.collederas.kroll.core.configentry.versioning

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "active_versions")
class ActiveVersionEntity(

    @Id
    @Column(name = "environment_id", nullable = false)
    val environmentId: UUID,

    @Column(name = "version", nullable = false)
    var version: String,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "updated_by", nullable = false)
    var updatedBy: UUID,
)

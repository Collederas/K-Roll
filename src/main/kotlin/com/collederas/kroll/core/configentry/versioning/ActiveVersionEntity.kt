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

    // pointer to published version
    @Column(name = "active_version_id")
    var activeVersionId: UUID? = null,

    // draft workspace
    @Column(name = "draft_json")
    var draftJson: String? = null,

    @Column(name = "draft_updated_at")
    var draftUpdatedAt: Instant? = null,

    @Column(name = "draft_updated_by")
    var draftUpdatedBy: UUID? = null,

    // publish metadata
    @Column(name = "published_at")
    var publishedAt: Instant? = null,

    @Column(name = "published_by")
    var publishedBy: UUID? = null,
)

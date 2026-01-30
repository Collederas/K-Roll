package com.collederas.kroll.core.config.versioning

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
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
    @Type(JsonType::class)
    @Column(name = "draft_json", columnDefinition = "jsonb")
    var draftJson: JsonNode? = null,
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

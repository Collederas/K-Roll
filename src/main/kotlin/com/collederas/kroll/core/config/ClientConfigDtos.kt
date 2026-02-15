package com.collederas.kroll.core.config

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

data class ClientConfigFetchResponseDto(
    val meta: ClientConfigMetaDto,
    val values: Map<String, Any?>,
)

data class ClientConfigMetaDto(
    @field:JsonProperty("schema_version")
    val schemaVersion: Int,
    @field:JsonProperty("active_snapshot_id")
    val activeSnapshotId: UUID,
    @field:JsonProperty("active_snapshot_hash")
    val activeSnapshotHash: String,
    @field:JsonProperty("published_at")
    val publishedAt: Instant,
    val label: String? = null,
)

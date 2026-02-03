package com.collederas.kroll.core.config.draft

import com.collederas.kroll.core.config.versioning.ActiveVersionEntity
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

data class ConfigDraftResponseDto(
    val environmentId: UUID,
    val draftJson: JsonNode,
    // Read-only context
    val baseVersion: BaseVersionDto?,
    // Editor state hints
    val isDirty: Boolean,
    val lastModifiedAt: Instant?,
) {
    companion object {
        fun from(active: ActiveVersionEntity): ConfigDraftResponseDto {
            val draft =
                active.draftJson
                    ?: error("draftJson must be initialized")

            return ConfigDraftResponseDto(
                environmentId = active.environmentId,
                draftJson = draft,
                baseVersion =
                    active.activeVersionId?.let { versionId ->
                        BaseVersionDto(
                            versionId = versionId,
                            publishedAt =
                                active.publishedAt
                                    ?: error(
                                        "active version $versionId has no publishedAt",
                                    ),
                        )
                    },
                isDirty = active.isDraftDirty(),
                lastModifiedAt = active.draftUpdatedAt,
            )
        }
    }
}

fun ActiveVersionEntity.isDraftDirty(): Boolean =
    draftJson != null &&
        publishedAt != null &&
        draftUpdatedAt != null &&
        draftUpdatedAt?.isAfter(publishedAt) == true

data class BaseVersionDto(
    val versionId: UUID,
    val publishedAt: Instant,
)

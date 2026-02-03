package com.collederas.kroll.core.config.versioning

import com.collederas.kroll.core.config.diff.DiffEntry
import com.collederas.kroll.core.config.entry.ConfigType
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

fun ConfigVersion(
    seq: Long,
    id: UUID = UUID.randomUUID(),
    envId: UUID = UUID.randomUUID(),
    label: String = "v$seq",
    contractHash: String,
    parentHash: String? = null,
    createdAt: Instant = Instant.now(),
    createdBy: UUID = UUID.randomUUID(),
    changeLog: String? = null,
): ConfigVersionEntity =
    ConfigVersionEntity(
        id = id,
        environmentId = envId,
        versionSequence = seq,
        versionLabel = label,
        contractHash = contractHash,
        parentHash = parentHash,
        createdAt = createdAt,
        createdBy = createdBy,
        changeLog = changeLog,
    )


fun ActiveVersion(
    environmentId: UUID = UUID.randomUUID(),
    activeVersionId: UUID? = null,
    draftJson: JsonNode? = null,
    draftUpdatedAt: Instant? = null,
    draftUpdatedBy: UUID? = null,
    publishedAt: Instant? = null,
    publishedBy: UUID? = null,
): ActiveVersionEntity =
    ActiveVersionEntity(
        environmentId = environmentId,
        activeVersionId = activeVersionId,
        draftJson = draftJson,
        draftUpdatedAt = draftUpdatedAt,
        draftUpdatedBy = draftUpdatedBy,
        publishedAt = publishedAt,
        publishedBy = publishedBy,
    )


fun ConfigEntry(
    type: ConfigType,
    value: Any,
) = DiffEntry(
    type = type,
    value = value,
)

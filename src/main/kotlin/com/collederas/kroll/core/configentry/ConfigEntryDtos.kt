package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.configentry.entries.ConfigType
import java.time.Instant
import java.util.*

data class CreateConfigEntryDto(
    val key: String,
    val value: String,
    val type: ConfigType,
    val activeFrom: Instant? = null,
    val activeUntil: Instant? = null,
)

data class UpdateConfigEntryDto(
    val value: String? = null,
    val type: ConfigType? = null,
    val activeFrom: Instant? = null,
    val activeUntil: Instant? = null,
    val changeDescription: String? = null,
    val clearActiveFrom: Boolean = false,
    val clearActiveUntil: Boolean = false,
)

data class ConfigEntryResponseDto(
    val key: String,
    val value: String,
    val type: ConfigType,
    val activeFrom: Instant?,
    val activeUntil: Instant?,
    val environmentId: UUID,
)

data class ConfigVersionDto(
    val id: UUID,

    val versionSequence: Long,

    val versionLabel: String,

    val environmentId: UUID,
    val createdAt: Instant,
    val createdBy: UUID?,

    val isActive: Boolean,
    val publishedAt: Instant?,

    val contractHash: String,
    val parentHash: String?,

    val changeLog: String?,
)


data class VersionDetailsDto(
    val id: UUID,
    val versionSequence: Long,
    val versionLabel: String,

    val createdAt: Instant,
    val createdBy: UUID?,
    val createdByName: String?,

    val contractHash: String,
    val parentHash: String?,

    val changeLog: String?,

    val snapshotJson: String,
    val diffPayload: String?,
)


data class ConfigDiff(
    val fromVersionId: UUID,
    val toVersionId: UUID,
    val added: Set<String>,
    val removed: Set<String>,
    val typeChanged: Set<String>,
)


data class ConfigSnapshotResponseDto(
    val versionId: UUID,
    val versionSequence: Long,
    val versionLabel: String,
    val entries: List<ConfigEntryResponseDto>,
)


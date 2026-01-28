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
    val id: String,
    val version: String,
    val environmentId: UUID,
    val createdAt: Instant,
    val createdBy: UUID?,
    val isActive: Boolean,
    val contractHash: String,
    val notes: String?,
)

data class VersionDetailsDto(
    val version: String,
    val createdAt: Instant,
    val createdBy: UUID?,
    val contractHash: String,
    val notes: String?,
    val snapshotJson: String,
)

data class ConfigDiff(
    val added: Set<String>,
    val removed: Set<String>,
    val typeChanged: Set<String>,
)

data class ConfigSnapshotResponseDto(
    val versionId: String,
    val environmentId: UUID,
    val entries: List<ConfigEntryResponseDto>,
)

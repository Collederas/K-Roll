package com.collederas.kroll.core.config

import com.collederas.kroll.core.config.entry.ConfigType
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
    val createdByName: String?,
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

data class ChangedKeyDto(
    val key: String,
    val oldType: String,
    val newType: String,
    val oldValue: Any?,
    val newValue: Any?,
)

data class ConfigDiffDto(
    val fromVersion: String,
    val toVersion: String,
    val added: Set<String>,
    val removed: Set<String>,
    val typeChanged: List<ChangedKeyDto>,
    val valueChanged: List<ChangedKeyDto>,
)

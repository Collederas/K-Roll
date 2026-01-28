package com.collederas.kroll.core.configentry

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
    val environmentId: UUID,
    val createdAt: Instant,
    val createdBy: UUID?,
    val isActive: Boolean,
    val contractHash: String,
    val notes: String?,
)

data class ConfigSnapshotResponseDto(
    val versionId: String,
    val environmentId: UUID,
    val entries: List<ConfigEntryResponseDto>,
)

sealed interface ConfigEntryDiffDto {
    val key: String

    data class Added(
        override val key: String,
        val new: ConfigEntryResponseDto,
    ) : ConfigEntryDiffDto

    data class Removed(
        override val key: String,
        val old: ConfigEntryResponseDto,
    ) : ConfigEntryDiffDto

    data class Changed(
        override val key: String,
        val old: ConfigEntryResponseDto,
        val new: ConfigEntryResponseDto,
        val semantic: SemanticDiffDto,
    ) : ConfigEntryDiffDto
}

sealed interface SemanticDiffDto {
    object Same : SemanticDiffDto
    object Different : SemanticDiffDto
    data class Invalid(val cause: String) : SemanticDiffDto
}

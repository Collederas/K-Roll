package com.collederas.kroll.core.configentry.dto

import com.collederas.kroll.core.configentry.ConfigType
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

data class EffectiveConfigDto(
    val configurations: Map<String, Any>,
)

package com.collederas.kroll.core.config.audit

import com.collederas.kroll.core.config.entry.ConfigType
import java.time.Instant
import java.util.UUID

data class ConfigEntrySnapshot(
    val key: String,
    val type: ConfigType,
    val value: String,
    val activeFrom: Instant?,
    val activeUntil: Instant?,
    val environmentId: UUID,
    val changeDescription: String?,
    val changedBy: UUID?,
)

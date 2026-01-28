package com.collederas.kroll.core.configentry.audit

import com.collederas.kroll.core.configentry.ConfigType
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

package com.collederas.kroll.core.configentry.audit

import java.util.UUID

data class ConfigEntryUpdatedEvent(
    val configEntryId: UUID,
    val environmentId: UUID,
    val changedBy: UUID,
    val changeDescription: String?,
    val snapshot: String,
)

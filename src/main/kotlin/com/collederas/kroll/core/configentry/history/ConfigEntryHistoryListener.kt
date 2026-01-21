package com.collederas.kroll.core.configentry.history

import com.collederas.kroll.core.configentry.ConfigEntryUpdatedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ConfigEntryHistoryListener(
    private val historyRepository: ConfigEntryHistoryRepository,
) {
    @EventListener
    fun onConfigUpdated(event: ConfigEntryUpdatedEvent) {
        val historyEntry =
            ConfigEntryHistoryEntity(
                configEntryId = event.configEntryId,
                environmentId = event.environmentId,
                changedBy = event.changedBy,
                changeDescription = event.changeDescription,
                configSnapshot = event.snapshot,
            )

        historyRepository.save(historyEntry)
    }
}

import com.collederas.kroll.core.configentry.ConfigEntryUpdatedEvent
import com.collederas.kroll.core.configentry.history.ConfigEntryHistoryEntity
import com.collederas.kroll.core.configentry.history.ConfigEntryHistoryRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ConfigEntryHistoryListener(
    private val historyRepository: ConfigEntryHistoryRepository,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
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

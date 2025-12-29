package com.collederas.kroll.core.configentry.history

import com.collederas.kroll.core.configentry.history.ConfigEntryHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ConfigEntryHistoryRepository : JpaRepository<ConfigEntryHistoryEntity, UUID>

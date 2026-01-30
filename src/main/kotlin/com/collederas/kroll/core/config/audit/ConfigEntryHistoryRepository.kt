package com.collederas.kroll.core.config.audit

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ConfigEntryHistoryRepository : JpaRepository<ConfigEntryHistoryEntity, UUID>

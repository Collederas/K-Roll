package com.collederas.kroll.core.configentry.history

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.*

@Entity
@Table(name = "config_entry_history")
class ConfigEntryHistoryEntity(
    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),
    @Column(name = "config_entry_id", nullable = false)
    val configEntryId: UUID,
    @Column(name = "environment_id", nullable = false)
    val environmentId: UUID,
    @Column(name = "changed_by", nullable = false)
    val changedBy: UUID,
    @Column(name = "changed_at", nullable = false)
    val changedAt: Instant = Instant.now(),
    @Column(name = "change_description")
    val changeDescription: String? = null,
    //    @JdbcTypeCode(SqlTypes.JSON)  to add whenever field is made JSONB in database
    @Column(name = "config_snapshot", columnDefinition = "TEXT", nullable = false)
    val configSnapshot: String,
)

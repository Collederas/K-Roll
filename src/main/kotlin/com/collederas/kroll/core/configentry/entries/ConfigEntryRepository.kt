package com.collederas.kroll.core.configentry.entries

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface ConfigEntryRepository : JpaRepository<ConfigEntryEntity, UUID> {
    fun existsByEnvironmentIdAndConfigKey(
        environmentId: UUID,
        configKey: String,
    ): Boolean

    fun findAllByEnvironmentId(environmentId: UUID): List<ConfigEntryEntity>

    fun findByEnvironmentIdAndConfigKey(
        environmentId: UUID,
        configKey: String,
    ): ConfigEntryEntity?
}

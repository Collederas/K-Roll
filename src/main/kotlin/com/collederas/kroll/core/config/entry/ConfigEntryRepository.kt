package com.collederas.kroll.core.config.entry

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
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

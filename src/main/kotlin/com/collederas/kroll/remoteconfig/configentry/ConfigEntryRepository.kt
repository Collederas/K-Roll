package com.collederas.kroll.remoteconfig.configentry

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ConfigEntryRepository : JpaRepository<ConfigEntryEntity, UUID> {
    fun findAllByEnvironmentId(environmentId: UUID): List<ConfigEntryEntity>
    fun findByEnvironmentIdAndConfigKey(environmentId: UUID, configKey: String): ConfigEntryEntity?
    fun existsByEnvironmentIdAndConfigKey(environmentId: UUID, configKey: String): Boolean
}
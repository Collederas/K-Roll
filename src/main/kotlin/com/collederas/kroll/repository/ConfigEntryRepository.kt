package com.collederas.kroll.repository

import com.collederas.kroll.entity.ConfigEntryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConfigEntryRepository : JpaRepository<ConfigEntryEntity, UUID> {
    fun findAllByEnvironmentId(environmentId: UUID): List<ConfigEntryEntity>
    fun findByEnvironmentIdAndConfigKey(environmentId: UUID, configKey: String): ConfigEntryEntity?
    fun existsByEnvironmentIdAndConfigKey(environmentId: UUID, configKey: String): Boolean
}
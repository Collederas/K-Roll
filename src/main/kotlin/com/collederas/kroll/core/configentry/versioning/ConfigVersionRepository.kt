package com.collederas.kroll.core.configentry.versioning

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ConfigVersionRepository : JpaRepository<ConfigVersionEntity, UUID> {

    fun findAllByEnvironmentId(environmentId: UUID): List<ConfigVersionEntity>

    fun findByEnvironmentId(environmentId: UUID): List<ConfigVersionEntity>

    fun existsByEnvironmentIdAndVersion(
        environmentId: UUID,
        version: String,
    ): Boolean

    fun findByEnvironmentIdAndVersion(
        environmentId: UUID,
        version: String,
    ) : ConfigVersionEntity?

    fun findLatestVersionByEnvironmentId(environmentId: UUID): ConfigVersionEntity?
}

package com.collederas.kroll.core.configentry.versioning

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ConfigVersionRepository : JpaRepository<ConfigVersionEntity, UUID> {

    fun findAllByEnvironmentId(environmentId: UUID): List<ConfigVersionEntity>

    fun findByEnvironmentIdAndVersionLabel(environmentId: UUID, versionLabel: String): List<ConfigVersionEntity>

    fun findAllByEnvironmentIdOrderByVersionSequenceDesc(
        environmentId: UUID
    ): List<ConfigVersionEntity>

    fun findTopByEnvironmentIdOrderByVersionSequenceDesc(
        environmentId: UUID
    ): ConfigVersionEntity?

    fun findLatestVersionByEnvironmentId(environmentId: UUID): ConfigVersionEntity?
}

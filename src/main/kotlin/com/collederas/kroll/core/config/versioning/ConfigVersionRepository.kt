package com.collederas.kroll.core.config.versioning

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ConfigVersionRepository : JpaRepository<ConfigVersionEntity, UUID> {
    fun findAllByEnvironmentIdOrderByVersionSequenceDesc(environmentId: UUID): List<ConfigVersionEntity>

    fun findTopByEnvironmentIdOrderByVersionSequenceDesc(environmentId: UUID): ConfigVersionEntity?

    fun findLatestByEnvironmentId(environmentId: UUID): ConfigVersionEntity? =
        findTopByEnvironmentIdOrderByVersionSequenceDesc(environmentId)
}

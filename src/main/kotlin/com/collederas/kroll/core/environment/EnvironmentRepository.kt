package com.collederas.kroll.core.environment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface EnvironmentRepository : JpaRepository<EnvironmentEntity, UUID> {
    fun findAllByProjectId(projectId: UUID): List<EnvironmentEntity>

    fun existsByIdAndProjectOwnerId(
        envId: UUID,
        ownerId: UUID,
    ): Boolean
}

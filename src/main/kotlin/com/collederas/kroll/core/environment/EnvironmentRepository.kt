package com.collederas.kroll.core.environment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface EnvironmentRepository : JpaRepository<EnvironmentEntity, UUID> {
    fun findAllByProjectOwnerId(ownerId: UUID): List<EnvironmentEntity>

    fun findAllByProjectId(projectId: UUID): List<EnvironmentEntity>

    fun existsByProjectIdAndName(
        projectId: UUID,
        name: String,
    ): Boolean
}

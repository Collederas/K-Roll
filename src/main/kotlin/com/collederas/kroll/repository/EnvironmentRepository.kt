package com.collederas.kroll.repository

import com.collederas.kroll.entity.EnvironmentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EnvironmentRepository : JpaRepository<EnvironmentEntity, UUID> {
    fun findAllByProjectId(projectId: UUID): List<EnvironmentEntity>
}
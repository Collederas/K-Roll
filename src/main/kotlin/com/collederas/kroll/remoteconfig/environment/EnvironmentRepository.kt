package com.collederas.kroll.remoteconfig.environment

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface EnvironmentRepository : JpaRepository<EnvironmentEntity, UUID> {
    fun findAllByProjectId(projectId: UUID): List<EnvironmentEntity>
}

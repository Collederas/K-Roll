package com.collederas.kroll.remoteconfig.project

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProjectRepository : JpaRepository<ProjectEntity, UUID> {
    fun existsByOwnerId(ownerId: UUID): Boolean
    fun findAllByOwnerId(ownerId: UUID): List<ProjectEntity>
}
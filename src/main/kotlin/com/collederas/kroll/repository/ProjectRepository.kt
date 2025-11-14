package com.collederas.kroll.repository

import com.collederas.kroll.entity.ProjectEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectRepository : JpaRepository<ProjectEntity, UUID> {
    fun existsByOwnerId(ownerId: UUID): Boolean
    fun findAllByOwnerId(ownerId: UUID): List<ProjectEntity>
}
package com.collederas.kroll.core.project

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectRepository : JpaRepository<ProjectEntity, UUID> {
    fun existsByOwnerId(ownerId: UUID): Boolean

    fun existsByOwnerIdAndName(
        ownerId: UUID,
        name: String,
    ): Boolean

    fun findAllByOwnerId(ownerId: UUID): List<ProjectEntity>
}

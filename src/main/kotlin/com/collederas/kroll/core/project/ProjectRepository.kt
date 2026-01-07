package com.collederas.kroll.core.project

import com.collederas.kroll.user.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProjectRepository : JpaRepository<ProjectEntity, UUID> {
    fun existsByOwnerId(ownerId: UUID): Boolean

    fun findAllByOwnerId(ownerId: UUID): List<ProjectEntity>

    fun existsByOwnerIdAndName(
        ownerId: UUID,
        name: String,
    ): Boolean

    fun existsByIdAndOwnerId(projectId: UUID, ownerId: UUID): Boolean
    fun owner(owner: AppUser): MutableList<ProjectEntity>
}

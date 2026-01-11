package com.collederas.kroll.core.project

import com.collederas.kroll.user.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProjectRepository : JpaRepository<ProjectEntity, UUID> {
    fun findAllByOwnerId(ownerId: UUID): List<ProjectEntity>

    fun existsByOwnerIdAndName(
        ownerId: UUID,
        name: String,
    ): Boolean

    fun findByIdAndOwnerId(
        id: UUID,
        ownerId: UUID,
    ): ProjectEntity?

    fun owner(owner: AppUser): MutableList<ProjectEntity>
}

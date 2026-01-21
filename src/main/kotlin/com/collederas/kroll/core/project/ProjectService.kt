package com.collederas.kroll.core.project

import com.collederas.kroll.core.exceptions.ProjectAlreadyExistsException
import com.collederas.kroll.core.exceptions.ProjectNotFoundException
import com.collederas.kroll.core.project.dto.CreateProjectDto
import com.collederas.kroll.core.project.dto.ProjectDto
import com.collederas.kroll.user.AppUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ProjectService(
    private val repo: ProjectRepository,
) {
    fun resolveOwnedProject(
        projectId: UUID,
        userId: UUID,
    ): ProjectEntity {
        val project =
            repo.findByIdAndOwnerId(projectId, userId)
                ?: throw ProjectNotFoundException("Project with ID $projectId not found")
        return project
    }

    @Transactional(readOnly = true)
    fun list(ownerId: UUID): List<ProjectDto> =
        repo.findAllByOwnerId(ownerId).map { ProjectDto(it.id, it.name, it.createdAt, it.owner.username) }

    @Transactional(readOnly = true)
    fun get(
        ownerId: UUID,
        projectId: UUID,
    ): ProjectDto {
        val project = resolveOwnedProject(projectId, ownerId)
        return ProjectDto(project.id, project.name, project.createdAt, project.owner.username)
    }

    @Transactional
    fun create(
        owner: AppUser,
        projectDto: CreateProjectDto,
    ): ProjectDto {
        if (repo.existsByOwnerIdAndName(owner.id, projectDto.name)) {
            throw ProjectAlreadyExistsException()
        }

        val project =
            ProjectEntity(
                name = projectDto.name,
                owner = owner,
            )
        repo.save(project)

        return ProjectDto(project.id, project.name, project.createdAt, project.owner.username)
    }

    @Transactional
    fun delete(
        ownerId: UUID,
        projectId: UUID,
    ) {
        val project = resolveOwnedProject(projectId, ownerId)
        repo.delete(project)
    }
}

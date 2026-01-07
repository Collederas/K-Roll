package com.collederas.kroll.core.project

import com.collederas.kroll.core.exceptions.OwnerAlreadyHasProjectException
import com.collederas.kroll.core.exceptions.ProjectAlreadyExistsException
import com.collederas.kroll.core.project.dto.CreateProjectDto
import com.collederas.kroll.core.project.dto.ProjectDto
import com.collederas.kroll.user.AppUser
import org.springframework.stereotype.Service
import java.util.*

@Service
class ProjectService(
    private val repo: ProjectRepository,
    private val projectAccessGuard: ProjectAccessGuard,
) {
    fun list(ownerId: UUID): List<ProjectDto> = repo.findAllByOwnerId(ownerId).map { ProjectDto(it.id, it.name) }

    fun create(
        owner: AppUser,
        projectDto: CreateProjectDto,
    ): ProjectDto {
        if (repo.existsByOwnerIdAndName(owner.id, projectDto.name)) {
            throw OwnerAlreadyHasProjectException(
                "Owner ${owner.id} already has a project",
            )
        }

        if (repo.existsByOwnerIdAndName(owner.id, projectDto.name)) {
            throw ProjectAlreadyExistsException(
                "Project with name '${projectDto.name}' already exists for this owner",
            )
        }

        val project =
            ProjectEntity(
                name = projectDto.name,
                owner = owner,
            )
        repo.save(project)

        return ProjectDto(project.id, project.name)
    }

    fun delete(ownerId: UUID, projectId: UUID) {
        projectAccessGuard.requireOwner(projectId, ownerId)
        repo.deleteById(projectId)
    }
}

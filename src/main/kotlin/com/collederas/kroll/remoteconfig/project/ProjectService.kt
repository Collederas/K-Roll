package com.collederas.kroll.remoteconfig.project

import com.collederas.kroll.remoteconfig.exceptions.ProjectAlreadyExistsException
import com.collederas.kroll.remoteconfig.project.dto.CreateProjectDto
import com.collederas.kroll.remoteconfig.project.dto.ProjectDto
import com.collederas.kroll.user.AppUser
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProjectService(private val repo: ProjectRepository) {
    fun list(): List<ProjectDto> = repo.findAll().map { ProjectDto(it.id, it.name) }

    fun create(
        owner: AppUser,
        projectDto: CreateProjectDto,
    ): ProjectDto {
        if (repo.existsByOwnerId(owner.id)) {
            throw ProjectAlreadyExistsException(
                "Project with name '${projectDto.name}' already exists.",
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

    fun delete(projectId: UUID) {
        repo.deleteById(projectId)
    }
}

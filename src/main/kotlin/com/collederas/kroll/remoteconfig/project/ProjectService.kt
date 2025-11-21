package com.collederas.kroll.remoteconfig.project

import com.collederas.kroll.remoteconfig.project.dto.CreateProjectDto
import com.collederas.kroll.remoteconfig.project.dto.ProjectDto
import com.collederas.kroll.user.AppUser
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ProjectService (
    private val repo: ProjectRepository
) {

    fun list(): List<ProjectDto> =
        repo.findAll().map { ProjectDto(it.id, it.name) }

    fun create(owner: AppUser, projectDto: CreateProjectDto): ProjectDto {
        val now = Instant.now()

        val project = ProjectEntity(
            id = UUID.randomUUID(),
            name = projectDto.name,
            owner = owner,
            createdAt = now,
            updatedAt = now
        )
        repo.save(project)

        return ProjectDto(project.id, project.name)
    }
}
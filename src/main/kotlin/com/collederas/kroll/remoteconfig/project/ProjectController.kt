package com.collederas.kroll.remoteconfig.project

import com.collederas.kroll.remoteconfig.project.dto.CreateProjectDto
import com.collederas.kroll.remoteconfig.project.dto.ProjectDto
import com.collederas.kroll.security.user.AuthUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/projects")
@Tag(name = "Project Management", description = "Endpoints for managing projects")
class ProjectController(private val projectService: ProjectService) {
    @GetMapping
    @Operation(
        summary = "List all projects",
        description = "Returns a list of all projects with their IDs and names",
    )
    fun list(): List<ProjectDto> = projectService.list()

    @PostMapping
    @Operation(
        summary = "Create a new project",
        description = "Creates a project for the authenticated user",
    )
    fun create(
        @AuthenticationPrincipal user: AuthUserDetails,
        @RequestBody dto: CreateProjectDto,
    ): ProjectDto = projectService.create(user.getUser(), dto)

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project", description = "Deletes the project with the given ID")
    fun delete(
        @PathVariable id: java.util.UUID,
    ) {
        projectService.delete(id)
    }
}

package com.collederas.kroll.api

import com.collederas.kroll.core.project.ProjectService
import com.collederas.kroll.core.project.dto.CreateProjectDto
import com.collederas.kroll.core.project.dto.ProjectDto
import com.collederas.kroll.security.identity.AuthUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Project Management", description = "Endpoints for managing projects")
class ProjectController(
    private val projectService: ProjectService,
) {
    @GetMapping("/{id}")
    @Operation(summary = "Get project details", description = "Returns details for the project with the given ID")
    fun get(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): ProjectDto = projectService.get(authUser.getId(), id)

    @GetMapping
    @Operation(
        summary = "List all projects",
        description = "Returns a list of all projects with their IDs and names",
    )
    fun list(
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): List<ProjectDto> = projectService.list(authUser.getId())

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
        @PathVariable id: UUID,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ) {
        projectService.delete(authUser.getId(), id)
    }
}

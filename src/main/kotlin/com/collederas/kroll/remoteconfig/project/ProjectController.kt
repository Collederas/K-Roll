package com.collederas.kroll.remoteconfig.project

import com.collederas.kroll.remoteconfig.project.dto.CreateProjectDto
import com.collederas.kroll.remoteconfig.project.dto.ProjectDto
import com.collederas.kroll.security.AuthUserDetails
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/projects")
class ProjectController (
    val projectService: ProjectService
) {
    @GetMapping
    fun  list(): List<ProjectDto> = projectService.list()

    @PostMapping
    fun create(
        @AuthenticationPrincipal user: AuthUserDetails,
        @RequestBody dto: CreateProjectDto
    ): ProjectDto = projectService.create(user.getUser(), dto)
}
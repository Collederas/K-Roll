package com.collederas.kroll.api

import com.collederas.kroll.core.environment.EnvironmentService
import com.collederas.kroll.core.environment.dto.CreateEnvironmentDto
import com.collederas.kroll.core.environment.dto.EnvironmentResponseDto
import com.collederas.kroll.security.identity.AuthUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/admin/projects/{projectId}/environments")
@Tag(name = "Environment Management", description = "Endpoints for managing environments")
class EnvironmentController(
    private val environmentService: EnvironmentService,
) {
    @GetMapping
    @Operation(summary = "List environments", description = "List all environments for a project")
    fun list(
        @PathVariable projectId: UUID,
        @AuthenticationPrincipal authUser: AuthUserDetails
    ): List<EnvironmentResponseDto> {
        return environmentService.list(projectId, authUser.getId())
    }

    @PostMapping
    @Operation(summary = "Create environment", description = "Create a new environment for a project")
    fun create(
        @PathVariable projectId: UUID,
        @RequestBody dto: CreateEnvironmentDto,
        @AuthenticationPrincipal authUser: AuthUserDetails
    ): EnvironmentResponseDto {
        return environmentService.create(authUser.getId(), projectId, dto)
    }
}

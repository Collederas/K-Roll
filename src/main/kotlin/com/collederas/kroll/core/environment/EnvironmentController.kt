package com.collederas.kroll.core.environment

import com.collederas.kroll.core.environment.dto.CreateEnvironmentDto
import com.collederas.kroll.core.environment.dto.EnvironmentResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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
    ): List<EnvironmentResponseDto> {
        return environmentService.list(projectId)
    }

    @PostMapping
    @Operation(summary = "Create environment", description = "Create a new environment for a project")
    fun create(
        @PathVariable projectId: UUID,
        @RequestBody dto: CreateEnvironmentDto,
    ): EnvironmentResponseDto {
        return environmentService.create(projectId, dto)
    }
}

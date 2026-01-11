package com.collederas.kroll.api

import com.collederas.kroll.core.environment.EnvironmentService
import com.collederas.kroll.core.environment.dto.CreateEnvironmentDto
import com.collederas.kroll.core.environment.dto.EnvironmentResponseDto
import com.collederas.kroll.security.identity.AuthUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/environments")
@Tag(name = "Environment Management", description = "Endpoints for managing environments")
class EnvironmentController(
    private val environmentService: EnvironmentService,
) {
    @GetMapping
    @Operation(
        summary = "List environments",
        description = "List all environments, optionally scoped to a project",
    )
    fun list(
        @RequestParam(required = false) projectId: UUID?,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): List<EnvironmentResponseDto> = environmentService.list(projectId, authUser.getId())

    @GetMapping("/{id}")
    @Operation(summary = "Get environment", description = "Get a specific environment by ID")
    fun get(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): EnvironmentResponseDto = environmentService.get(id, authUser.getId())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create environment", description = "Create a new environment for a project")
    fun create(
        @Valid @RequestBody dto: CreateEnvironmentDto,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): EnvironmentResponseDto = environmentService.create(authUser.getId(), dto)
}

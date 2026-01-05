package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.configentry.dto.ConfigEntryResponseDto
import com.collederas.kroll.core.configentry.dto.CreateConfigEntryDto
import com.collederas.kroll.core.configentry.dto.UpdateConfigEntryDto
import com.collederas.kroll.security.user.AuthUserDetails
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/admin/environments/{envId}/configs")
@Tag(name = "Config Entry Management", description = "Endpoints for managing config entries")
class AdminConfigController(
    private val configEntryService: ConfigEntryService,
) {
    @GetMapping
    @Operation(summary = "List config entries", description = "List all config entries for an environment")
    fun list(
        @PathVariable envId: UUID,
    ): List<ConfigEntryResponseDto> {
        return configEntryService.list(envId)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create config entry", description = "Create a new config entry")
    fun create(
        @PathVariable envId: UUID,
        @RequestBody dto: CreateConfigEntryDto,
        @AuthenticationPrincipal user: AuthUserDetails,
    ): ConfigEntryResponseDto {
        return configEntryService.create(envId, user.getId(), dto)
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update config entry", description = "Update an existing config entry")
    fun update(
        @PathVariable envId: UUID,
        @PathVariable key: String,
        @RequestBody dto: UpdateConfigEntryDto,
        @AuthenticationPrincipal user: AuthUserDetails,
    ): ConfigEntryResponseDto {
        return configEntryService.update(user.getId(), envId, key, dto)
    }

    // TODO: add authprincipal as argument in service for auditing
    @DeleteMapping("/{key}")
    @Operation(summary = "Delete config entry", description = "Delete a config entry")
    fun delete(
        @PathVariable envId: UUID,
        @PathVariable key: String,
    ) {
        configEntryService.delete(envId, key)
    }
}

package com.collederas.kroll.api

import com.collederas.kroll.core.configentry.ConfigEntryService
import com.collederas.kroll.core.configentry.dto.ConfigEntryResponseDto
import com.collederas.kroll.core.configentry.dto.CreateConfigEntryDto
import com.collederas.kroll.core.configentry.dto.UpdateConfigEntryDto
import com.collederas.kroll.security.identity.AuthUserDetails
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
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): List<ConfigEntryResponseDto> = configEntryService.list(authUser.getId(), envId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create config entry", description = "Create a new config entry")
    fun create(
        @PathVariable envId: UUID,
        @RequestBody dto: CreateConfigEntryDto,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): ConfigEntryResponseDto = configEntryService.create(authUser.getId(), envId, dto)

    @PutMapping("/{key}")
    @Operation(summary = "Update config entry", description = "Update an existing config entry")
    fun update(
        @PathVariable envId: UUID,
        @PathVariable key: String,
        @RequestBody dto: UpdateConfigEntryDto,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): ConfigEntryResponseDto = configEntryService.update(authUser.getId(), envId, key, dto)

    @DeleteMapping("/{key}")
    @Operation(summary = "Delete config entry", description = "Delete a config entry")
    fun delete(
        @PathVariable envId: UUID,
        @PathVariable key: String,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ) = configEntryService.delete(authUser.getId(), envId, key)
}

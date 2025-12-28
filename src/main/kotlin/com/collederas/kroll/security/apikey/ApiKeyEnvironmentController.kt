package com.collederas.kroll.security.apikey

import com.collederas.kroll.security.apikey.dto.ApiKeyMetadataDto
import com.collederas.kroll.security.apikey.dto.CreateApiKeyResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID


@RestController
@RequestMapping("/admin/environments/{envId}/api-keys")
@Tag(name = "API Key Management", description = "Endpoints for managing API keys")
class ApiKeyEnvironmentController(
    private val apiKeyService: ApiKeyService,
) {
    @GetMapping
    @Operation(summary = "List API keys", description = "List all API keys for an environment")
    fun list(
        @PathVariable envId: UUID,
    ): List<ApiKeyMetadataDto> {
        return apiKeyService.list(envId)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create API key", description = "Create a new API key for an environment")
    fun create(
        @PathVariable envId: UUID,
        @RequestBody expiresAt: Instant,
    ): CreateApiKeyResponseDto {
        return apiKeyService.create(envId, expiresAt)
    }
}


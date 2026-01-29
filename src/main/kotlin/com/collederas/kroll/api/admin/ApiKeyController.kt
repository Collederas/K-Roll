package com.collederas.kroll.api.admin

import com.collederas.kroll.security.apikey.ApiKeyService
import com.collederas.kroll.security.apikey.dto.ApiKeyMetadataDto
import com.collederas.kroll.security.apikey.dto.CreateApiKeyRequest
import com.collederas.kroll.security.apikey.dto.CreateApiKeyResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/environments/{envId}/api-keys")
@Tag(name = "API Key Management", description = "Endpoints for managing API keys")
class ApiKeyController(
    private val apiKeyService: ApiKeyService,
) {
    @GetMapping
    @Operation(summary = "List API keys", description = "List all API keys for an environment")
    fun list(
        @PathVariable envId: UUID,
    ): List<ApiKeyMetadataDto> = apiKeyService.list(envId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create API key", description = "Create a new API key for an environment")
    fun create(
        @PathVariable envId: UUID,
        @RequestBody dto: CreateApiKeyRequest,
    ): CreateApiKeyResponseDto = apiKeyService.create(envId, dto)

    @DeleteMapping("{apiKeyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete API key", description = "Delete an API key")
    fun delete(
        @PathVariable apiKeyId: UUID,
    ) = apiKeyService.delete(apiKeyId)
}

package com.collederas.kroll.security.apikey

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/admin/apikey")
class ApiKeyController(
    private val apiKeyService: ApiKeyService,
) {
    @DeleteMapping("{apiKeyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete API key", description = "Delete an API key")
    fun delete(
        @PathVariable apiKeyId: UUID,
    ) {
        runCatching { apiKeyService.delete(apiKeyId) }
    }
}

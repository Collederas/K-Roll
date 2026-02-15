package com.collederas.kroll.api.client

import com.collederas.kroll.core.config.ClientConfigFetchResponseDto
import com.collederas.kroll.core.config.ClientConfigQueryService
import com.collederas.kroll.security.apikey.identity.GameClientPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("!contractgen")
@RequestMapping("/client/config")
@Tag(name = "Client API", description = "Endpoints for game clients")
class ClientConfigController(
    private val clientConfigQueryService: ClientConfigQueryService,
) {
    @PostMapping("/fetch")
    @Operation(summary = "Fetch configuration", description = "Returns effective config for the environment")
    fun fetchConfig(auth: Authentication): ClientConfigFetchResponseDto {
        val principal = auth.principal as GameClientPrincipal
        return clientConfigQueryService.fetchPublishedConfig(principal.environmentId)
    }
}

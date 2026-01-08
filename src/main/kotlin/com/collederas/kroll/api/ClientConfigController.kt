package com.collederas.kroll.api

import com.collederas.kroll.core.configentry.ConfigEntryService
import com.collederas.kroll.security.apikey.authentication.GameClientPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/client/config")
@Tag(name = "Client API", description = "Endpoints for game clients")
class ClientConfigController(
    private val clientConfigService: ConfigEntryService,
) {
    @PostMapping("/fetch")
    @Operation(summary = "Fetch configuration", description = "Returns effective config for the environment")
    fun fetchConfig(auth: Authentication): Map<String, Any> {
        val principal = auth.principal as GameClientPrincipal
        return clientConfigService.fetchEffectiveConfig(principal.environmentId)
    }
}

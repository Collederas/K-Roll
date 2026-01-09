package com.collederas.kroll.security.apikey.authentication

import com.collederas.kroll.security.apikey.ApiKeyService
import com.collederas.kroll.security.apikey.identity.GameClientPrincipal
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

@Component
class ApiKeyAuthenticationProvider(
    private val apiKeyService: ApiKeyService,
) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication? {
        val rawKey = authentication.credentials as String

        val result = apiKeyService.validate(rawKey)

        // *Assuming that if the key id is set, the environment id is set as well*
        if (result.apiKeyId == null) {
            throw BadCredentialsException("Invalid API key")
        }

        val principal =
            GameClientPrincipal(
                result.environmentId!!,
                result.apiKeyId,
            )

        return ApiKeyAuthenticationToken(
            principal,
            null,
            result.roles.map { SimpleGrantedAuthority(it) },
        )
    }

    override fun supports(authentication: Class<*>) =
        ApiKeyAuthenticationToken::class.java.isAssignableFrom(authentication)
}

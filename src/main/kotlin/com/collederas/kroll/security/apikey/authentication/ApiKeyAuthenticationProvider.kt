package com.collederas.kroll.security.apikey.authentication

import com.collederas.kroll.security.apikey.ApiKeyService
import com.collederas.kroll.security.apikey.dto.ApiKeyAuthResult
import com.collederas.kroll.security.apikey.identity.GameClientPrincipal
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

@Component
class ApiKeyAuthenticationProvider(
    private val apiKeyService: ApiKeyService,
) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication {
        val rawKey = authentication.credentials as String

        return when (val result = apiKeyService.validate(rawKey)) {
            is ApiKeyAuthResult.Valid -> {
                val principal =
                    GameClientPrincipal(
                        result.environmentId,
                        result.apiKeyId,
                    )

                ApiKeyAuthenticationToken(
                    principal,
                    null,
                    result.roles.map { SimpleGrantedAuthority(it) },
                )
            }

            ApiKeyAuthResult.Invalid ->
                throw BadCredentialsException("Invalid API key")

            ApiKeyAuthResult.Expired ->
                throw CredentialsExpiredException("API key expired")
        }
    }

    override fun supports(authentication: Class<*>) =
        ApiKeyAuthenticationToken::class.java.isAssignableFrom(authentication)
}

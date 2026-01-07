package com.collederas.kroll.security.apikey.authentication

import com.collederas.kroll.security.apikey.ApiKeyService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

data class GameClientPrincipal(
    val environmentId: UUID,
    val apiKeyId: UUID,
)

@Component
class ApiKeyAuthenticationFilter(
    private val apiKeyService: ApiKeyService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val existing = SecurityContextHolder.getContext().authentication
        if (existing != null && existing.isAuthenticated) {
            filterChain.doFilter(request, response)
            return
        }

        val rawApiKey = request.getHeader("X-Api-Key")
        if (rawApiKey.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        val authResult = apiKeyService.validate(rawApiKey)
        if (authResult.apiKeyId == null || authResult.environmentId == null) {
            filterChain.doFilter(request, response)
            return
        }

        val principal = GameClientPrincipal(authResult.environmentId, authResult.apiKeyId)
        val authorities = authResult.roles.map { SimpleGrantedAuthority(it) }

        val authToken =
            UsernamePasswordAuthenticationToken(
                principal,
                null,
                authorities,
            )

        SecurityContextHolder.getContext().authentication = authToken
        filterChain.doFilter(request, response)
    }
}

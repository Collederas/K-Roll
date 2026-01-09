package com.collederas.kroll.security.apikey.authentication

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.web.filter.OncePerRequestFilter

class ApiKeyAuthenticationFilter(
    private val authenticationManager: AuthenticationManager,
    private val authenticationEntryPoint: AuthenticationEntryPoint,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val existing = SecurityContextHolder.getContext().authentication
        val shouldAuthenticateWithApiKey =
            existing == null ||
                !existing.isAuthenticated ||
                existing is AnonymousAuthenticationToken

        if (shouldAuthenticateWithApiKey) {
            try {
                val apiKey = request.getHeader("X-API-KEY")

                if (apiKey != null) {
                    val authentication = ApiKeyAuthenticationToken(apiKey)
                    val authResult = authenticationManager.authenticate(authentication)
                    SecurityContextHolder.getContext().authentication = authResult
                }

                chain.doFilter(request, response)
            } catch (ex: AuthenticationException) {
                SecurityContextHolder.clearContext()
                authenticationEntryPoint.commence(request, response, ex)
            }
        } else {
            chain.doFilter(request, response)
        }
    }
}

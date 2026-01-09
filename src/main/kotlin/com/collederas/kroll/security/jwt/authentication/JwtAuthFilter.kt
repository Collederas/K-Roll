package com.collederas.kroll.security.jwt.authentication

import com.collederas.kroll.security.identity.AuthUserDetailsService
import com.collederas.kroll.security.jwt.JwtTokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

/**
 * _devnotes: "Part of Spring filter chain logic: filters requests looking for a valid
 * JWT token to populate the SecurityContext with an authenticated principal."
 * */
class JwtAuthFilter(
    private val jwtService: JwtTokenService,
    private val userDetailsService: AuthUserDetailsService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val existing = SecurityContextHolder.getContext().authentication
        val shouldAuthenticateWithJwt =
            existing == null ||
                !existing.isAuthenticated ||
                existing is AnonymousAuthenticationToken

        if (shouldAuthenticateWithJwt) {
            extractBearerToken(request)?.let { token ->
                jwtService.validateAndGetUserId(token)?.let { userId ->
                    val userDetails = userDetailsService.loadUserById(userId)

                    val authToken =
                        UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.authorities,
                        ).apply {
                            details = WebAuthenticationDetailsSource().buildDetails(request)
                        }

                    SecurityContextHolder.getContext().authentication = authToken
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractBearerToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        val parts = header.split(" ", limit = 2)

        return if (parts.size == 2 && parts[0].equals("Bearer", ignoreCase = true)) {
            parts[1].trim().takeIf { it.isNotBlank() }
        } else {
            null
        }
    }
}

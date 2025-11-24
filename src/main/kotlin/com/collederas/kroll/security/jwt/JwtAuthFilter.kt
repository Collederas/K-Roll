package com.collederas.kroll.security.jwt

import com.collederas.kroll.security.AuthUserDetailsService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * _devnotes: "Part of Spring filter chain logic: filters requests looking for a valid
 * JWT token to populate the SecurityContext with an authenticated principal."
 * */
@Component
class JwtAuthFilter(
    private val jwtService: JwtTokenService,
    private val userDetailsService: AuthUserDetailsService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // If authentication exists, just use it
        val existing = SecurityContextHolder.getContext().authentication
        if (existing != null) {
            filterChain.doFilter(request, response)
            return
        }

        val header = request.getHeader("Authorization") ?: ""
        val parts = header.split(" ")

        val validScheme = parts.size == 2 && parts[0] == "Bearer"
        if (!validScheme) {
            filterChain.doFilter(request, response)
            return
        }

        val token = parts[1].trim()
        val userId = jwtService.validateAndGetUserId(token)

        if (userId != null) {
            val userDetails = userDetailsService.loadUserById(userId)

            // TODO: Cache?
            val authToken = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
            authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authToken
        }

        filterChain.doFilter(request, response)
    }
}

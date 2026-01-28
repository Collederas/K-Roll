package com.collederas.kroll.security.jwt.authentication

import com.collederas.kroll.security.identity.AuthUserDetailsService
import com.collederas.kroll.security.jwt.JwtTokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

/**
 * _devnotes: "Part of Spring filter chain logic: filters requests looking for a valid
 * JWT token to populate the SecurityContext with an authenticated principal."
 * */
class JwtAuthFilter(
    private val jwtService: JwtTokenService,
    private val userDetailsService: AuthUserDetailsService,
    private val authenticationEntryPoint: AuthenticationEntryPoint,
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.servletPath.startsWith("/auth/login")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (shouldAuthenticateWithJwt()) {
            attemptJwtAuthentication(request, response)
        }

        filterChain.doFilter(request, response)
    }

    private fun shouldAuthenticateWithJwt(): Boolean {
        val existing = SecurityContextHolder.getContext().authentication
        return existing == null ||
            !existing.isAuthenticated ||
            existing is AnonymousAuthenticationToken
    }

    private fun attemptJwtAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val token = extractBearerToken(request) ?: return
        val userId = jwtService.validateAndGetUserId(token) ?: return

        try {
            val userDetails = userDetailsService.loadUserById(userId)
            setAuthentication(request, userDetails)
        } catch (ex: UsernameNotFoundException) {
            SecurityContextHolder.clearContext()
            authenticationEntryPoint.commence(request, response, ex)
        }
    }

    private fun setAuthentication(
        request: HttpServletRequest,
        userDetails: org.springframework.security.core.userdetails.UserDetails,
    ) {
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

package com.collederas.kroll.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * _devnotes: "The ExceptionTranslationFilter handles any AuthenticationException and AccessDeniedException
 * thrown within the filter chain. If an AuthenticationException is caught for an unauthenticated user,
 * the filter invokes the AuthenticationEntryPoint. The exception will trigger the response defined in the
 * commence() method, which sends a challenge (e.g., 401 Unauthorized).
 *
 * This is essentially ran when a non authenticated request is detected.
 * It provides information to the client on how to authenticate
 * using the WWW-Authenticate method as per HTTP Challenge Spec."
 */
@Component
class AuthEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException?,
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"")

        val body =
            mapOf(
                "timestamp" to System.currentTimeMillis(),
                "status" to HttpServletResponse.SC_UNAUTHORIZED,
                "error" to "Unauthorized",
                "message" to (authException?.message ?: "Full authentication is required to access this resource"),
                "path" to request.requestURI,
            )

        response.writer.use {
            it.write(objectMapper.writeValueAsString(body))
        }
    }
}

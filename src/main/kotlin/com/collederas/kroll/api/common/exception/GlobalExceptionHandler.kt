package com.collederas.kroll.api.common.exception

import com.collederas.kroll.core.exceptions.ApiException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Primary handler for all custom API exceptions.
     * The sealed class hierarchy ensures consistent handling.
     */
    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ProblemDetail =
        createProblemDetail(
            status = ex.status,
            errorCode = ex.errorCode,
            title = formatTitle(ex.errorCode),
            detail = ex.message,
            additionalProperties = ex.additionalProperties(),
        )

    /**
     * Handle Spring Security authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ProblemDetail =
        createProblemDetail(
            status = HttpStatus.UNAUTHORIZED,
            errorCode = "AUTHENTICATION_FAILED",
            title = "Authentication Failed",
            detail = ex.message ?: "Authentication failed",
        )

    /**
     * Handle Bean Validation errors from @Valid annotated parameters.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ProblemDetail {
        val fieldErrors =
            ex.bindingResult.fieldErrors.map { fieldError ->
                mapOf(
                    "field" to fieldError.field,
                    "message" to (fieldError.defaultMessage ?: "Invalid value"),
                    "rejectedValue" to fieldError.rejectedValue,
                )
            }

        return createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            errorCode = "VALIDATION_FAILED",
            title = "Validation Failed",
            detail = "One or more fields failed validation",
            additionalProperties = mapOf("fieldErrors" to fieldErrors),
        )
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * Logs the full stack trace but returns a generic message to clients.
     * Note: Spring's ResponseStatusException and servlet exceptions are excluded
     * to allow Spring's default handling for 404s on non-existent endpoints.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ProblemDetail {
        // Let Spring handle its own exceptions (e.g., NoHandlerFoundException for 404)
        if (ex.javaClass.name.startsWith("org.springframework.web.servlet")) {
            throw ex
        }

        logger.error("Unexpected error occurred", ex)

        return createProblemDetail(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = "INTERNAL_ERROR",
            title = "Internal Server Error",
            detail = "An unexpected error occurred. Please try again later.",
        )
    }

    /**
     * Factory method for creating consistent ProblemDetail responses.
     */
    private fun createProblemDetail(
        status: HttpStatus,
        errorCode: String,
        title: String,
        detail: String,
        additionalProperties: Map<String, Any?> = emptyMap(),
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).also { problem ->
            problem.title = title
            problem.setProperty("error_code", errorCode)
            problem.instance = URI("/errors/${errorCode.lowercase().replace('_', '-')}")

            additionalProperties.forEach { (key, value) ->
                problem.setProperty(key, value)
            }
        }

    /**
     * Convert error code to human-readable title.
     * Example: "PROJECT_NOT_FOUND" -> "Project Not Found"
     */
    private fun formatTitle(errorCode: String): String =
        errorCode
            .split('_')
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
}

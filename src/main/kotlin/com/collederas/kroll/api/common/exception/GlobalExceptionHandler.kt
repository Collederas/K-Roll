package com.collederas.kroll.api.common.exception

import com.collederas.kroll.core.exceptions.* // Importing the "Pure" Core exceptions
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ==================================================================
    // DOMAIN EXCEPTION MAPPING (Core -> HTTP)
    // ==================================================================

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ProblemDetail = mapToProblem(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException): ProblemDetail = mapToProblem(HttpStatus.CONFLICT, ex)

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(ex: BadRequestException): ProblemDetail = mapToProblem(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException): ProblemDetail = mapToProblem(HttpStatus.FORBIDDEN, ex)

    // ==================================================================
    // FRAMEWORK / INFRASTRUCTURE EXCEPTIONS
    // ==================================================================

    /**
     * Handle Spring Security authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ProblemDetail =
        createProblemDetail(
            status = HttpStatus.UNAUTHORIZED,
            errorCode = "AUTHENTICATION_FAILED",
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
            detail = "One or more fields failed validation",
            additionalProperties = mapOf("fieldErrors" to fieldErrors),
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ProblemDetail =
        createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            errorCode = "INVALID_REQUEST_BODY",
            detail = "Request body is missing or malformed",
        )

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpMessageNotReadable(ex: HttpRequestMethodNotSupportedException): ProblemDetail =
        createProblemDetail(
            status = HttpStatus.METHOD_NOT_ALLOWED,
            errorCode = "METHOD_NOT_ALLOWED",
            detail = "Method not allowed",
        )

    /**
     * Catch-all handler for unexpected exceptions.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(ex: Exception): ProblemDetail {
        // Let Spring handle its own control flow exceptions
        if (ex.javaClass.name.startsWith("org.springframework.web.servlet")) {
            throw ex
        }

        logger.error("Unexpected error occurred", ex)

        return createProblemDetail(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = "INTERNAL_ERROR",
            detail = "An unexpected error occurred. Please try again later.",
        )
    }

    // ==================================================================
    // HELPERS
    // ==================================================================

    /**
     * Bridge method to convert a Core ApiException (Domain) to a ProblemDetail (Web).
     */
    private fun mapToProblem(
        status: HttpStatus,
        ex: KrollException,
    ): ProblemDetail =
        createProblemDetail(
            status = status,
            errorCode = ex.errorCode,
            detail = ex.message,
            additionalProperties = ex.additionalDetails(),
        )

    /**
     * Factory method for creating consistent ProblemDetail responses.
     */
    private fun createProblemDetail(
        status: HttpStatus,
        errorCode: String,
        detail: String,
        additionalProperties: Map<String, Any?> = emptyMap(),
    ): ProblemDetail {
        val title = formatTitle(errorCode)

        return ProblemDetail.forStatusAndDetail(status, detail).also { problem ->
            problem.title = title
            problem.setProperty("error_code", errorCode)
            problem.instance = URI("/errors/${errorCode.lowercase().replace('_', '-')}")

            additionalProperties.forEach { (key, value) ->
                problem.setProperty(key, value)
            }
        }
    }

    private fun formatTitle(errorCode: String): String =
        errorCode
            .split('_')
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
}

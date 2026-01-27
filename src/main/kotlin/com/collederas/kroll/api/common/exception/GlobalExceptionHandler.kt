package com.collederas.kroll.api.common.exception

import com.collederas.kroll.exceptions.BadRequestException
import com.collederas.kroll.exceptions.ConflictException
import com.collederas.kroll.exceptions.ForbiddenException
import com.collederas.kroll.exceptions.KrollException
import com.collederas.kroll.exceptions.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ==================================================================
    // DOMAIN EXCEPTION MAPPING (Core -> HTTP)
    // ==================================================================

    @ExceptionHandler(KrollException::class)
    fun handleCoreException(ex: KrollException): ProblemDetail = mapToProblem(mapStatus(ex), ex)

    // ==================================================================
    // FRAMEWORK / INFRASTRUCTURE EXCEPTIONS
    // ==================================================================

    @ExceptionHandler(NoHandlerFoundException::class, NoResourceFoundException::class)
    fun handleNotFound(ex: Exception): ProblemDetail =
        createProblemDetail(
            status = HttpStatus.NOT_FOUND,
            errorCode = "RESOURCE_NOT_FOUND",
            detail = ex.message ?: "The requested URL was not found on this server.",
        )

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ProblemDetail =
        createProblemDetail(
            status = HttpStatus.UNAUTHORIZED,
            errorCode = "AUTHENTICATION_FAILED",
            detail = ex.message ?: "Authentication failed",
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ProblemDetail {
        val fieldErrors =
            ex.bindingResult.fieldErrors.map {
                mapOf(
                    "field" to it.field,
                    "message" to (it.defaultMessage ?: "Invalid value"),
                    "rejectedValue" to it.rejectedValue,
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
    @Suppress("UNUSED_PARAMETER")
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ProblemDetail =
        createProblemDetail(
            status = HttpStatus.BAD_REQUEST,
            errorCode = "INVALID_REQUEST_BODY",
            detail = "Request body is missing or malformed",
        )

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    @Suppress("UNUSED_PARAMETER")
    fun handleMethodNotSupported(ex: HttpRequestMethodNotSupportedException): ProblemDetail =
        createProblemDetail(
            status = HttpStatus.METHOD_NOT_ALLOWED,
            errorCode = "METHOD_NOT_ALLOWED",
            detail = "Method not allowed",
        )

    @ExceptionHandler(Exception::class)
    @Suppress("UNUSED_PARAMETER")
    fun handleUnexpectedException(ex: Exception): ProblemDetail =
        createProblemDetail(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            errorCode = "INTERNAL_ERROR",
            detail = "An unexpected error occurred. Please try again later.",
        )

    // ==================================================================
    // HELPERS
    // ==================================================================

    private fun mapStatus(ex: KrollException): HttpStatus =
        when (ex) {
            is NotFoundException -> HttpStatus.NOT_FOUND
            is ConflictException -> HttpStatus.CONFLICT
            is BadRequestException -> HttpStatus.BAD_REQUEST
            is ForbiddenException -> HttpStatus.FORBIDDEN
        }

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

    private fun createProblemDetail(
        status: HttpStatus,
        errorCode: String,
        detail: String,
        additionalProperties: Map<String, Any?> = emptyMap(),
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).also { problem ->
            problem.title = formatTitle(errorCode)
            problem.setProperty("error_code", errorCode)
            problem.instance = URI("/errors/${errorCode.lowercase().replace('_', '-')}")
            additionalProperties.forEach(problem::setProperty)
        }

    private fun formatTitle(errorCode: String): String =
        errorCode
            .split('_')
            .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
}

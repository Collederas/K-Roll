package com.collederas.kroll.api.common.exception

import com.collederas.kroll.core.exceptions.ForbiddenException
import com.collederas.kroll.core.exceptions.InvalidConfigTypeException
import com.collederas.kroll.core.exceptions.ProjectAlreadyExistsException
import com.collederas.kroll.security.apikey.exception.InvalidApiKeyExpiryException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(e: AuthenticationException): ProblemDetail =
        ProblemDetail
            .forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                e.message ?: "Authentication failed",
            ).also {
                it.title = "Authentication Error"
                it.instance = URI("/errors/auth-failed")
            }

    @ExceptionHandler(InvalidApiKeyExpiryException::class)
    fun handleInvalidApiKeyExpiry(ex: InvalidApiKeyExpiryException): ProblemDetail =
        ProblemDetail
            .forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.message ?: "Invalid expiry",
            ).also {
                it.title = "Invalid Expiry"
                it.setProperty("error_code", "INVALID_API_KEY_EXPIRY")
            }

    @ExceptionHandler(ProjectAlreadyExistsException::class)
    fun handleDuplicateProject(ex: ProjectAlreadyExistsException): ProblemDetail =
        ProblemDetail
            .forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.message ?: "Project already exists",
            ).also {
                it.title = "Project Conflict"
                it.setProperty("error_code", "PROJECT_EXISTS")
            }

    @ExceptionHandler(InvalidConfigTypeException::class)
    fun handleInvalidConfigType(ex: InvalidConfigTypeException): ProblemDetail =
        ProblemDetail
            .forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.message ?: "Invalid config type",
            ).also {
                it.title = "Invalid Config Type"
                it.setProperty("error_code", "INVALID_CONFIG_TYPE")
            }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ProblemDetail =
        ProblemDetail
            .forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.message
                    ?: "A resource with these unique identifiers already exists.",
            ).also {
                it.title = "Conflict"
            }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(ex: ForbiddenException): ProblemDetail =
        ProblemDetail
            .forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.message ?: "Access to this resource is forbidden.",
            ).also {
                it.title = "Forbidden"
            }
}

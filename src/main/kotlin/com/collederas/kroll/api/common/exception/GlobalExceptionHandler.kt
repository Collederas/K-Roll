package com.collederas.kroll.api.common.exception

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
    fun handleAuthenticationException(e: AuthenticationException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            e.message
                ?: "Authentication failed",
        ).apply {
            title = "Authentication Error"
            instance = URI("/errors/auth-failed")
        }
    }

    @ExceptionHandler(InvalidApiKeyExpiryException::class)
    fun handleInvalidApiKeyExpiry(ex: InvalidApiKeyExpiryException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.message
                ?: "Invalid expiry",
        ).apply {
            title = "Invalid Expiry"
            setProperty("error_code", "INVALID_API_KEY_EXPIRY")
        }
    }

    @ExceptionHandler(ProjectAlreadyExistsException::class)
    fun handleDuplicateProject(ex: ProjectAlreadyExistsException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.message
                ?: "Project already exists",
        ).apply {
            title = "Project Conflict"
            setProperty("error_code", "PROJECT_EXISTS")
        }
    }

    @ExceptionHandler(InvalidConfigTypeException::class)
    fun handleInvalidConfigType(ex: InvalidConfigTypeException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.message
                ?: "Invalid config type",
        ).apply {
            title = "Invalid Config Type"
            setProperty("error_code", "INVALID_CONFIG_TYPE")
        }
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "A resource with these unique identifiers already exists."
        )
        problem.title = "Conflict"
        return problem
    }
}

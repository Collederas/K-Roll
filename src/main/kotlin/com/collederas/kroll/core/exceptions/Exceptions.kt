package com.collederas.kroll.core.exceptions

import org.springframework.http.HttpStatus

/**
 * Base sealed class for all API exceptions.
 * Each exception carries its own HTTP status, error code, and message.
 * This enables a single exception handler with consistent error responses.
 */
sealed class ApiException(
    val status: HttpStatus,
    val errorCode: String,
    override val message: String,
) : RuntimeException(message) {
    /**
     * Additional properties to include in the ProblemDetail response.
     * Override in subclasses that need extra fields (e.g., validation errors).
     */
    open fun additionalProperties(): Map<String, Any?> = emptyMap()
}

// ==================== 404 NOT FOUND ====================

sealed class NotFoundException(
    errorCode: String,
    message: String,
) : ApiException(HttpStatus.NOT_FOUND, errorCode, message)

class ProjectNotFoundException(
    message: String = "Project not found",
) : NotFoundException("PROJECT_NOT_FOUND", message)

class EnvironmentNotFoundException(
    message: String = "Environment not found",
) : NotFoundException("ENVIRONMENT_NOT_FOUND", message)

class ConfigEntryNotFoundException(
    message: String = "Config entry not found",
) : NotFoundException("CONFIG_ENTRY_NOT_FOUND", message)

// ==================== 409 CONFLICT ====================

sealed class ConflictException(
    errorCode: String,
    message: String,
) : ApiException(HttpStatus.CONFLICT, errorCode, message)

class ProjectAlreadyExistsException(
    message: String = "A project with this name already exists",
) : ConflictException("PROJECT_ALREADY_EXISTS", message)

class EnvironmentAlreadyExistsException(
    message: String = "An environment with this name already exists in this project",
) : ConflictException("ENVIRONMENT_ALREADY_EXISTS", message)

// ==================== 400 BAD REQUEST ====================

sealed class BadRequestException(
    errorCode: String,
    message: String,
) : ApiException(HttpStatus.BAD_REQUEST, errorCode, message)

class InvalidConfigTypeException(
    message: String = "Invalid configuration type",
) : BadRequestException("INVALID_CONFIG_TYPE", message)

class InvalidApiKeyExpiryException(
    message: String = "Invalid API key expiry",
) : BadRequestException("INVALID_API_KEY_EXPIRY", message)

class ConfigValidationException(
    val errors: List<String>,
    message: String = "Configuration validation failed",
) : BadRequestException("CONFIG_VALIDATION_FAILED", message) {
    override fun additionalProperties(): Map<String, Any?> = mapOf("errors" to errors)
}

// ==================== 403 FORBIDDEN ====================

class ForbiddenException(
    message: String = "Access to this resource is forbidden",
) : ApiException(HttpStatus.FORBIDDEN, "ACCESS_FORBIDDEN", message)

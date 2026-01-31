package com.collederas.kroll.exceptions

/**
 * Base sealed class for all Domain exceptions.
 */
sealed class KrollException(
    val errorCode: String,
    override val message: String,
) : RuntimeException(message) {
    /**
     * Pure data extensions.
     * The API layer will decide how to serialize this.
     */
    open fun additionalDetails(): Map<String, Any?> = emptyMap()
}

// ==================== NOT FOUND (404) CATEGORY ====================

sealed class NotFoundException(
    errorCode: String,
    message: String,
) : KrollException(errorCode, message)

class ProjectNotFoundException(
    message: String = "Project not found",
) : NotFoundException("PROJECT_NOT_FOUND", message)

class EnvironmentNotFoundException(
    message: String = "Environment not found",
) : NotFoundException("ENVIRONMENT_NOT_FOUND", message)

class ConfigEntryNotFoundException(
    message: String = "Config entry not found",
) : NotFoundException("CONFIG_ENTRY_NOT_FOUND", message)

// ==================== CONFLICT (409) CATEGORY ====================

sealed class ConflictException(
    errorCode: String,
    message: String,
) : KrollException(errorCode, message)

class ProjectAlreadyExistsException(
    message: String = "A project with this name already exists",
) : ConflictException("PROJECT_ALREADY_EXISTS", message)

class EnvironmentAlreadyExistsException(
    message: String = "Environment already exists",
) : ConflictException("ENVIRONMENT_ALREADY_EXISTS", message)

class EnvironmentHasActiveApiKeysException(
    message: String = "Environment cannot be deleted while it has active API keys",
) : ConflictException("ENVIRONMENT_HAS_ACTIVE_API_KEYS", message)

class ExistingUnpublishedDraft(
    message: String = "Promotion would discard existing draft state",
) : ConflictException("DRAFT_PRESENT", message)

// ==================== BAD REQUEST (400) CATEGORY ====================

sealed class BadRequestException(
    errorCode: String,
    message: String,
) : KrollException(errorCode, message)

class InvalidCredentialsException(
    message: String = "Invalid credentials",
) : BadRequestException("INVALID_CREDENTIALS", message)

class InvalidApiKeyExpiryException(
    message: String = "Invalid API key expiry",
) : BadRequestException("INVALID_API_KEY_EXPIRY", message)

class ConfigValidationException(
    val errors: List<String>,
    message: String = "Configuration validation failed",
) : BadRequestException("CONFIG_VALIDATION_FAILED", message) {
    override fun additionalDetails(): Map<String, Any?> = mapOf("errors" to errors)
}

// ==================== FORBIDDEN (403) CATEGORY ====================

class ForbiddenException(
    message: String = "Access to this resource is forbidden",
) : KrollException("ACCESS_FORBIDDEN", message)

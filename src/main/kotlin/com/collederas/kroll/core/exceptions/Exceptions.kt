package com.collederas.kroll.core.exceptions

class ProjectAlreadyExistsException(
    message: String? = null,
) : RuntimeException(message)

class ProjectNotFoundException(
    message: String? = null,
) : RuntimeException(message)

class EnvironmentNotFoundException(
    message: String? = null,
) : RuntimeException(message)

class ConfigEntryNotFoundException(
    message: String? = null,
) : RuntimeException(message)

class InvalidConfigTypeException(
    message: String? = null,
) : RuntimeException(message)

class ForbiddenException(
    message: String? = null,
) : RuntimeException(message)

package com.collederas.kroll.core.exceptions

class ProjectAlreadyExistsException(message: String) : RuntimeException(message)

class OwnerAlreadyHasProjectException(message: String) : RuntimeException(message)

class ProjectNotFoundException(message: String) : RuntimeException(message)

class EnvironmentNotFoundException(message: String) : RuntimeException(message)

class ConfigEntryNotFoundException(message: String) : RuntimeException(message)

class InvalidConfigTypeException(message: String) : RuntimeException(message)

class ForbiddenException(message: String) : RuntimeException(message)


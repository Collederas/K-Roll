package com.collederas.kroll.remoteconfig.exceptions

class ProjectAlreadyExistsException(message: String) : RuntimeException(message)
class ProjectNotFoundException(message: String) : RuntimeException(message)
class ApiKeyNotFoundException(message: String) : RuntimeException(message)


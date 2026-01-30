package com.collederas.kroll.core.config.validation

class ConfigValidationException(
    val violations: List<String>,
) : RuntimeException("Configuration validation failed")

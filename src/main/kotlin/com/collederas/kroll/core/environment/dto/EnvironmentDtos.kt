package com.collederas.kroll.core.environment.dto

import java.util.UUID

data class CreateEnvironmentDto(
    val name: String,
)

data class EnvironmentResponseDto(
    val id: UUID,
    val name: String,
    val projectId: UUID,
)

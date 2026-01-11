package com.collederas.kroll.core.project.dto

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.*

data class CreateProjectDto(
    @field:NotBlank val name: String,
)

data class ProjectDto(
    val id: UUID,
    val name: String,
    val createdAt: Instant,
)

package com.collederas.kroll.core.project.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateProjectDto(
    @field:NotBlank val name: String,
)

data class ProjectDto(
    val id: UUID,
    val name: String,
)

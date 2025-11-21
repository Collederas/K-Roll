package com.collederas.kroll.remoteconfig.project.dto

import java.util.UUID

data class CreateProjectDto(
    val name: String,
)

data class ProjectDto(
    val id: UUID,
    val name: String,
)
package com.collederas.kroll.user.dto

import java.util.UUID

data class BasicUserDto(
    val id: UUID,
    val username: String,
    val email: String,
)

package com.collederas.kroll.user.dto

import java.util.*

data class BasicUserDto(
    val id: UUID,
    val username: String,
    val email: String,
)

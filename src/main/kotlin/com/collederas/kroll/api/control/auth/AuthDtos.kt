package com.collederas.kroll.api.control.auth

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank val identifier: String,
    @field:NotBlank val password: String,
)

data class LoginResponse(
    val access: String,
    val refresh: String,
)

data class RefreshTokenRequest(
    val refresh: String,
)

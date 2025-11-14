package com.collederas.kroll.auth

import jakarta.validation.constraints.NotBlank
import org.aspectj.bridge.MessageUtil.fail
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Validated
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        return LoginResponse(authService.login(request.identifier, request.password))
    }
}

data class LoginRequest(
    @field:NotBlank val identifier: String,
    @field:NotBlank val password: String
)

data class LoginResponse(
    val token: String
    // TODO: refresh token
)
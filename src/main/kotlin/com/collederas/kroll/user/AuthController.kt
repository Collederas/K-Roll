package com.collederas.kroll.user

import com.collederas.kroll.user.dto.BasicUserDto
import jakarta.validation.constraints.NotBlank
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
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

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: AuthUserDetails): BasicUserDto {
        return BasicUserDto(
            id = user.getId(),
            username = user.username,
            email = user.getEmail()
        )
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
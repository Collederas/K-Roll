package com.collederas.kroll.api.control

import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.security.jwt.authentication.JwtAuthService
import com.collederas.kroll.user.dto.AppUserDto
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(
    name = "Authentication",
    description = "User authentication and session management using JWT access and refresh tokens",
)
@Validated
class AdminAuthController(
    private val authService: JwtAuthService,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): LoginResponse {
        val (accessToken, refreshToken) = authService.login(request.identifier, request.password)
        return LoginResponse(access = accessToken, refresh = refreshToken)
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: RefreshTokenRequest,
    ): LoginResponse {
        val (accessToken, refreshToken) = authService.refreshToken(request.refresh)
        return LoginResponse(access = accessToken, refresh = refreshToken)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @AuthenticationPrincipal principal: AuthUserDetails,
    ) {
        authService.revokeTokenFor(principal.getUser())
    }

    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal user: AuthUserDetails,
    ): AppUserDto =
        AppUserDto(
            id = user.getId(),
            username = user.username,
            email = user.getEmail(),
        )

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
}

package com.collederas.kroll.api.auth

import com.collederas.kroll.security.jwt.authentication.JwtAuthService
import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.user.dto.BasicUserDto
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
@Validated
class JwtAuthController(
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
    ): BasicUserDto {
        return BasicUserDto(
            id = user.getId(),
            username = user.username,
            email = user.getEmail(),
        )
    }
}

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

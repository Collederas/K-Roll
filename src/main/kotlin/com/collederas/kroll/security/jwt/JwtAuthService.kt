package com.collederas.kroll.security.jwt

import com.collederas.kroll.security.AuthUserDetails
import com.collederas.kroll.user.AppUser
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service

@Service
class JwtAuthService(
    private val authManager: AuthenticationManager,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
) {
    fun login(
        usernameOrEmail: String,
        password: String,
    ): Pair<String, String> {
        val auth = UsernamePasswordAuthenticationToken(usernameOrEmail, password)
        val authentication = authManager.authenticate(auth)

        val principal = authentication.principal as AuthUserDetails

        val accessToken = jwtTokenService.generateToken(principal.getId(), principal.username)
        val refreshToken = refreshTokenService.rotateAllTokensFor(principal.getUser())
        return accessToken to refreshToken
    }

    fun refreshToken(refreshToken: String): Pair<String, String> {
        val (user, newRefreshEntity) = refreshTokenService.rotateFromRefresh(refreshToken)
        val newAccessToken = jwtTokenService.generateToken(user.id, user.username)

        return newAccessToken to newRefreshEntity
    }

    fun revokeTokenFor(user: AppUser) {
        refreshTokenService.revokeTokensFor(user)
    }
}

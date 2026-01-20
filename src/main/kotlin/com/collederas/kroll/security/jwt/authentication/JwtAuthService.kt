package com.collederas.kroll.security.jwt.authentication

import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.security.jwt.JwtTokenService
import com.collederas.kroll.security.jwt.RefreshTokenService
import com.collederas.kroll.user.AppUser
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class JwtAuthService(
    private val authManager: AuthenticationManager,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
) {
    @Transactional
    fun login(
        usernameOrEmail: String,
        password: String,
    ): Pair<String, String> {
        val auth = UsernamePasswordAuthenticationToken(usernameOrEmail, password)
        val authentication =
            try {
                authManager.authenticate(auth)
            } catch (e: BadCredentialsException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credentials", e)
            }

        val principal = authentication.principal as AuthUserDetails

        val accessToken = jwtTokenService.generateToken(principal.getId(), principal.username)
        val refreshToken = refreshTokenService.rotateAllTokensFor(principal.getUser())
        return accessToken to refreshToken
    }

    @Transactional
    fun refreshToken(refreshToken: String): Pair<String, String> {
        val (user, newRefreshEntity) = refreshTokenService.rotateFromRefresh(refreshToken)
        val newAccessToken = jwtTokenService.generateToken(user.id, user.username)

        return newAccessToken to newRefreshEntity
    }

    fun revokeTokenFor(user: AppUser) {
        refreshTokenService.revokeTokensFor(user)
    }
}

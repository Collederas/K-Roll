package com.collederas.kroll.user

import com.collederas.kroll.security.jwt.JwtTokenService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authManager: AuthenticationManager,
    private val jwtService: JwtTokenService
) {
    fun login(usernameOrEmail: String, password: String): String {
        val auth = UsernamePasswordAuthenticationToken(usernameOrEmail, password)
        val authentication = authManager.authenticate(auth)

        val principal = authentication.principal as AuthUserDetails
        return jwtService.generateToken(principal.getId(), principal.username)
    }
}
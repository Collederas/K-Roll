package com.collederas.kroll.auth

import com.collederas.kroll.security.CustomUserDetails
import com.collederas.kroll.security.token.JwtTokenService
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
        val principal = authentication.principal as CustomUserDetails
        return jwtService.generateToken(principal.getId(), principal.username)
    }
}
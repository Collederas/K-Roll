package com.collederas.kroll.support

import com.collederas.kroll.security.apikey.ApiKeyService
import com.collederas.kroll.security.jwt.authentication.JwtAuthEntryPoint
import com.collederas.kroll.security.jwt.authentication.JwtAuthService
import com.collederas.kroll.security.jwt.JwtTokenService
import com.collederas.kroll.security.identity.AuthUserDetailsService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class TestSecurityConfig {

    @Bean
    fun authService(): JwtAuthService = mockk(relaxed = true)

    @Bean
    fun apiKeyService(): ApiKeyService = mockk(relaxed = true)

    @Bean
    fun jwtTokenService(): JwtTokenService = mockk(relaxed = true)

    @Bean
    fun authUserDetailsService(): AuthUserDetailsService = mockk(relaxed = true)

    @Bean
    fun jwtAuthEntryPoint(): JwtAuthEntryPoint = JwtAuthEntryPoint(objectMapper = ObjectMapper())
}

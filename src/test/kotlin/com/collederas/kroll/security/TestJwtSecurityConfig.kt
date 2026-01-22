package com.collederas.kroll.security

import com.collederas.kroll.security.identity.AuthUserDetailsService
import com.collederas.kroll.security.jwt.JwtTokenService
import com.collederas.kroll.security.jwt.authentication.JwtAuthFilter
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@TestConfiguration
class TestJwtSecurityConfig(
    private val jwtService: JwtTokenService,
    private val userDetailsService: AuthUserDetailsService,
) {
    @Bean
    fun jwtAuthFilter(authEntryPoint: AuthEntryPoint): JwtAuthFilter =
        JwtAuthFilter(jwtService, userDetailsService, authEntryPoint)

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun jwtTestChain(
        http: HttpSecurity,
        jwtAuthFilter: JwtAuthFilter,
    ): SecurityFilterChain {
        http
            .securityMatcher("/test/auth/jwt/**")
            .csrf { it.disable() }
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}

package com.collederas.kroll.security.jwt

import com.collederas.kroll.security.AuthEntryPoint
import com.collederas.kroll.security.identity.AuthUserDetailsService
import com.collederas.kroll.security.jwt.authentication.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class JwtSecurityConfig(
    private val authEntryPoint: AuthEntryPoint,
    private val jwtService: JwtTokenService,
    private val userDetailsService: AuthUserDetailsService,
) {
    @Bean
    fun jwtAuthFilter(): JwtAuthFilter = JwtAuthFilter(jwtService, userDetailsService)

    @Bean
    @Order(2)
    fun mainChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it
                    .requestMatchers("/api/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        "/auth/login",
                        "/auth/refresh",
                        "/error",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                    ).permitAll()
                it.requestMatchers("/auth/logout").authenticated()
                it.anyRequest().permitAll()
            }.exceptionHandling {
                it.authenticationEntryPoint(authEntryPoint)
            }.addFilterBefore(
                jwtAuthFilter(),
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}

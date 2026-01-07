package com.collederas.kroll.security.jwt

import com.collederas.kroll.security.identity.AuthUserDetailsService
import com.collederas.kroll.security.jwt.authentication.JwtAuthEntryPoint
import com.collederas.kroll.security.jwt.authentication.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class JwtSecurityConfig(
    private val jwtAuthEntryPoint: JwtAuthEntryPoint,
    private val jwtAuthFilter: JwtAuthFilter,
    private val userDetailsService: AuthUserDetailsService,
    private val passwordEncoder: BCryptPasswordEncoder,
) {
    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider =
        DaoAuthenticationProvider(userDetailsService).apply {
            setPasswordEncoder(passwordEncoder)
        }

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager =
        authConfig.authenticationManager

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
                it.requestMatchers("/admin/**").hasRole("ADMIN")
                it
                    .requestMatchers(
                        "/auth/login",
                        "/auth/refresh",
                        "/error",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                    ).permitAll()
                it.requestMatchers("/auth/logout").authenticated()
                it.anyRequest().denyAll()
            }.exceptionHandling {
                it.authenticationEntryPoint(jwtAuthEntryPoint)
            }.addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}

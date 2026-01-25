package com.collederas.kroll.security

import com.collederas.kroll.security.apikey.ApiKeyService
import com.collederas.kroll.security.apikey.authentication.ApiKeyAuthenticationProvider
import com.collederas.kroll.security.identity.AuthUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig {
    companion object {
        private const val CORS_MAX_AGE_SECONDS = 3600L
    }

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(daoAuthenticationProvider: DaoAuthenticationProvider): AuthenticationManager =
        ProviderManager(daoAuthenticationProvider)

    @Bean
    fun authenticationProvider(
        userDetailsService: AuthUserDetailsService,
        passwordEncoder: BCryptPasswordEncoder,
    ): DaoAuthenticationProvider =
        DaoAuthenticationProvider(userDetailsService).apply {
            setPasswordEncoder(passwordEncoder)
        }

    @Bean
    fun apiKeyAuthenticationProvider(apiKeyService: ApiKeyService): ApiKeyAuthenticationProvider =
        ApiKeyAuthenticationProvider(apiKeyService)

    @Bean
    fun corsConfigurationSource(env: Environment): CorsConfigurationSource {
        val config =
            CorsConfiguration().apply {
                allowedOrigins =
                    env.getProperty("cors.origins", Array<String>::class.java)?.toList()
                        ?: listOf("http://localhost:8080", "http://localhost:5173")
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("Authorization", "Content-Type", "X-Api-Key")
                exposedHeaders = listOf("WWW-Authenticate")
                allowCredentials = false
                maxAge = CORS_MAX_AGE_SECONDS
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}

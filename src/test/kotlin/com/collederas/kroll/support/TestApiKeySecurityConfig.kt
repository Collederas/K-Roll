package com.collederas.kroll.support

import com.collederas.kroll.security.AuthEntryPoint
import com.collederas.kroll.security.apikey.authentication.ApiKeyAuthenticationFilter
import com.collederas.kroll.security.apikey.authentication.ApiKeyAuthenticationProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@TestConfiguration
class TestApiKeySecurityConfig(
    private val apiKeyAuthenticationProvider: ApiKeyAuthenticationProvider,
) {
    @Bean
    fun authEntrypoint() = AuthEntryPoint(ObjectMapper())

    @Bean
    fun testAuthenticationManager(): AuthenticationManager = ProviderManager(apiKeyAuthenticationProvider)

    @Bean
    fun apiKeyAuthenticationFilter(
        authenticationManager: AuthenticationManager,
        authEntryPoint: AuthEntryPoint,
    ): ApiKeyAuthenticationFilter = ApiKeyAuthenticationFilter(authenticationManager, authEntryPoint)

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun clientTestChain(
        http: HttpSecurity,
        apiKeyAuthenticationFilter: ApiKeyAuthenticationFilter,
    ): SecurityFilterChain {
        http
            .securityMatcher("/test/auth/apikey/**")
            .csrf { it.disable() }
            .addFilterBefore(
                apiKeyAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}

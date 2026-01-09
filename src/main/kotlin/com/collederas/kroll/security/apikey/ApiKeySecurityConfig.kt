package com.collederas.kroll.security.apikey

import com.collederas.kroll.security.AuthEntryPoint
import com.collederas.kroll.security.apikey.authentication.ApiKeyAuthenticationFilter
import com.collederas.kroll.security.apikey.authentication.ApiKeyAuthenticationProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class ApiKeySecurityConfig {
    @Bean
    fun apiKeyAuthenticationFilter(
        apiKeyAuthenticationProvider: ApiKeyAuthenticationProvider,
        authEntryPoint: AuthEntryPoint,
    ): ApiKeyAuthenticationFilter {
        val manager = ProviderManager(apiKeyAuthenticationProvider)
        return ApiKeyAuthenticationFilter(manager, authEntryPoint)
    }

    @Bean
    @Order(1)
    fun clientChain(
        http: HttpSecurity,
        apiKeyAuthenticationFilter: ApiKeyAuthenticationFilter,
        authEntryPoint: AuthEntryPoint,
    ): SecurityFilterChain {
        http
            .securityMatcher("/client/**")
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.exceptionHandling {
                it.authenticationEntryPoint(authEntryPoint)
            }.authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/client/**").permitAll()
                it.anyRequest().hasRole("GAME_CLIENT")
            }.addFilterBefore(
                apiKeyAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            ).formLogin { it.disable() }
            .httpBasic { it.disable() }

        return http.build()
    }
}

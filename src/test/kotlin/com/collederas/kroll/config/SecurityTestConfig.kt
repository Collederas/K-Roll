package com.collederas.kroll.config

import com.collederas.kroll.security.apikey.ApiKeyAuthenticationFilter
import com.collederas.kroll.support.controllers.AuthIntrospectionController
import com.collederas.kroll.support.filters.PreAuthTestFilter
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@TestConfiguration
class SecurityTestConfig {

    @Bean
    fun preAuthTestFilter() = PreAuthTestFilter()

    @Bean
    fun authIntrospectionController() = AuthIntrospectionController()


    /**
     * Injects a filter that forces a test authentication before anything else.
     * This is useful to test how other filters behave when a request has already
     * been authenticated. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun clientTestChain(
        http: HttpSecurity,
        preAuthTestFilter: PreAuthTestFilter,
        apiKeyAuthenticationFilter: ApiKeyAuthenticationFilter
    ): SecurityFilterChain {
        http
            .securityMatcher("/test/auth/**")
            .csrf { it.disable() }
            .addFilterBefore(preAuthTestFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}

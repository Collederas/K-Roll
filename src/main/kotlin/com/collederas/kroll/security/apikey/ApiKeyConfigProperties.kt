package com.collederas.kroll.security.apikey

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@ConfigurationProperties(prefix = "auth.api-key")
@Validated
@Suppress("MagicNumber")
data class ApiKeyConfigProperties(
    val defaultLifetime: Duration = Duration.ofDays(30),
    val maxLifetime: Duration = Duration.ofDays(365),
) {
    @PostConstruct
    fun validate() {
        if (defaultLifetime > maxLifetime) {
            throw IllegalStateException(
                "Invalid configuration: defaultLifetime ($defaultLifetime) cannot exceed maxLifetime ($maxLifetime)",
            )
        }
    }
}

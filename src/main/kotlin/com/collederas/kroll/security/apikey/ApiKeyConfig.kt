package com.collederas.kroll.security.apikey

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "auth.api-key")
data class ApiKeyConfig(
    val maxLifetime: Duration = Duration.ofDays(365),
)

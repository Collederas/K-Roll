package com.collederas.kroll.bootstrap.validation

import org.hibernate.internal.CoreLogging.logger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment

class ProductionConfigValidator : EnvironmentPostProcessor {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        val logger: Logger = LoggerFactory.getLogger(javaClass)
        logger.info(">>> KROLL EnvironmentPostProcessor LOADED <<<")

        val profile = environment.getProperty("KROLL_PROFILE") ?: "prod"
        if (profile != "prod") return

        val missing = mutableListOf<String>()

        fun requireEnv(name: String) {
            if (environment.getProperty(name).isNullOrBlank()) {
                missing += name
            }
        }

        // JWT
        if (
            environment.getProperty("KROLL_JWT_SECRET").isNullOrBlank() &&
            environment.getProperty("KROLL_JWT_SECRET_FILE").isNullOrBlank()
        ) {
            missing += "KROLL_JWT_SECRET or KROLL_JWT_SECRET_FILE"
        }

        // DB
        requireEnv("KROLL_DB_URL")
        requireEnv("KROLL_DB_USER")

        if (
            environment.getProperty("KROLL_DB_PASSWORD").isNullOrBlank() &&
            environment.getProperty("KROLL_DB_PASSWORD_FILE").isNullOrBlank()
        ) {
            missing += "KROLL_DB_PASSWORD or KROLL_DB_PASSWORD_FILE"
        }

        if (missing.isNotEmpty()) {
            val message =
                buildString {
                    appendLine()
                    appendLine("KROLL FAILED TO START (invalid production configuration)")
                    appendLine()
                    appendLine("Missing required configuration:")
                    missing.forEach { appendLine("- $it") }
                    appendLine()
                    appendLine("If you intended to run locally, set:")
                    appendLine("  KROLL_PROFILE=dev")
                }

            throw IllegalStateException(message)
        }
    }
}

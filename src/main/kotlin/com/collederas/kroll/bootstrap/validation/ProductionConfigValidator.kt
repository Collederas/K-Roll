package com.collederas.kroll.bootstrap.validation

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment

class ProductionConfigValidator : EnvironmentPostProcessor {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        println(">>> KROLL EnvironmentPostProcessor LOADED <<<")

        val activeProfiles = environment.activeProfiles
        if ("prod" !in activeProfiles) return

        val missing = mutableListOf<String>()

        fun requireEnv(name: String) {
            if (environment.getProperty(name).isNullOrEmpty()) {
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

            println(environment.getProperty("contract.hash"))

            throw IllegalStateException(message)
        }
    }
}

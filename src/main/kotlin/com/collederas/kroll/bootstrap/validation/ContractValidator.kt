package com.collederas.kroll.bootstrap.validation

import jakarta.annotation.PostConstruct
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("prod")
class ContractValidator(
    private val buildProperties: BuildProperties,
) {
    @PostConstruct
    fun validate() {
        val contractHash = buildProperties["contract.hash"]

        if (contractHash.isNullOrBlank()) {
            error(
                """
                KROLL FAILED TO START (invalid production build)

                Missing embedded OpenAPI contract hash (build.contract.hash)

                This artifact was not built via the contract pipeline.
                """.trimIndent(),
            )
        }
    }
}

package com.collederas.kroll.api

import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MetaController(
    private val buildProperties: BuildProperties,
) {
    @GetMapping("/meta")
    fun meta(): Map<String, String> {
        val version = buildProperties.version
        val contractHash =
            buildProperties["contract.hash"]
                ?: error("Contract hash missing from build info")

        return mapOf(
            "version" to version,
            "contractHash" to contractHash,
        )
    }
}

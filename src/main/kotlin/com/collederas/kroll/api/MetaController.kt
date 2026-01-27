package com.collederas.kroll.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("!contractgen")
@PropertySource("classpath:contract.properties", ignoreResourceNotFound = true)
class MetaController(
    private val buildProperties: BuildProperties,
    @param:Value("\${kroll.contract.hash:dev-local}") private val contractHash: String
) {
    @GetMapping("/meta")
    fun meta(): Map<String, String> {
        return mapOf(
            "version" to buildProperties.version,
            "contractHash" to contractHash
        )
    }
}

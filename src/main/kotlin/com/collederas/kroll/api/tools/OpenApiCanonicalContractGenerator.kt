package com.collederas.kroll.api.tools

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.erdtman.jcs.JsonCanonicalizer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

object OpenApiCanonicalContractGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.isNotEmpty()) { "Path to OpenAPI spec must be provided" }

        val path = Path.of(args[0])
        val hashPath = path.resolveSibling("openapi.sha256")

        val mapper =
            JsonMapper
                .builder()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build()

        val tree = mapper.readTree(Files.readString(path))

        // Server URLs differ across environments and must not affect compatibility hashes
        (tree as? ObjectNode)?.remove("servers")

        val normalized =
            mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(tree)

        Files.writeString(
            path,
            normalized,
            StandardOpenOption.TRUNCATE_EXISTING,
        )

        val canonical =
            JsonCanonicalizer(mapper.writeValueAsString(tree)).encodedString

        Files.writeString(
            path.resolveSibling("openapi.canonical.json"),
            canonical,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
        )

        val hash =
            MessageDigest
                .getInstance("SHA-256")
                .digest(canonical.toByteArray())
                .joinToString("") { "%02x".format(it) }

        Files.writeString(
            path.resolveSibling("openapi.sha256"),
            hash,
        )
    }
}

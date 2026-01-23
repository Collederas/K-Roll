package com.collederas.kroll.api.tools

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object OpenApiNormalizer {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.isNotEmpty()) { "Path to OpenAPI spec must be provided" }

        val path = Path.of(args[0])

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
    }
}

package com.collederas.kroll.bootstrap.secrets

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

object SecretResolver {
    private val logger = LoggerFactory.getLogger(SecretResolver::class.java)

    fun resolve(
        valueOrPath: String?,
        name: String,
    ): String {
        if (valueOrPath == null) {
            throw IllegalStateException("Configuration '$name' is missing or empty")
        }

        val path = parsePathOrNull(valueOrPath)

        return if (path != null && isValidSecretFile(path)) {
            Files.readString(path).trim()
        } else {
            valueOrPath
        }
    }

    private fun parsePathOrNull(value: String): Path? =
        try {
            Path.of(value)
        } catch (e: InvalidPathException) {
            logger.debug("Value is not a valid path, treating as literal value: {}", value, e)
            null
        }

    private fun isValidSecretFile(path: Path): Boolean =
        Files.exists(path) &&
            Files.isReadable(path) &&
            !Files.isDirectory(path)
}

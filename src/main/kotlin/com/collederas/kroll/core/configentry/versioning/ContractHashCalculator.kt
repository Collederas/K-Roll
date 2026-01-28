package com.collederas.kroll.core.configentry.versioning

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class ContractHashCalculator(
    private val objectMapper: ObjectMapper,
) {
    fun compute(resolved: ResolvedConfig): String {
        val contract =
            resolved.values
                .mapValues { it.value.type.name }

        val bytes = objectMapper.writeValueAsBytes(contract)
        return sha256Hex(bytes)
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}

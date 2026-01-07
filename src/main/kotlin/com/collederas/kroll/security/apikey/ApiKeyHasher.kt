package com.collederas.kroll.security.apikey

import java.security.MessageDigest
import java.util.*

object ApiKeyHasher {
    /**
     * Hashes a raw key using SHA-256.
     * We use SHA-256 (fast) instead of BCrypt (slow) because
     * we might need to validate this on every single API call.
     */
    fun hash(rawKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(rawKey.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }
}

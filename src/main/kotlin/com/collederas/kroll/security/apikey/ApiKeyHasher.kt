package com.collederas.kroll.security.apikey

import com.collederas.kroll.crypto.Sha256

object ApiKeyHasher {
    /**
     * Hashes a raw key using SHA-256.
     * We use SHA-256 (fast) instead of BCrypt (slow) because
     * we might need to validate this on every single API call.
     */
    fun hash(rawKey: String): String = Sha256.hashBase64(rawKey.toByteArray(Charsets.UTF_8))
}

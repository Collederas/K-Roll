package com.collederas.kroll.crypto

import java.security.MessageDigest
import java.util.Base64

object Sha256 {
    fun hashBytes(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

    fun hashHex(bytes: ByteArray): String = hashBytes(bytes).joinToString("") { "%02x".format(it) }

    fun hashBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(hashBytes(bytes))
}

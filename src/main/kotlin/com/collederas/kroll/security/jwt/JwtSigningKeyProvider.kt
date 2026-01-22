package com.collederas.kroll.security.jwt

import com.collederas.kroll.bootstrap.secrets.SecretResolver
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtSigningKeyProvider(
    @param:Value("\${auth.jwt.secret}") private val jwtSecret: String,
) {
    val signingKey: SecretKey =
        run {
            val resolved = SecretResolver.resolve(jwtSecret, "KROLL_JWT_SECRET")
            val keyBytes =
                try {
                    Decoders.BASE64.decode(resolved)
                } catch (e: IllegalArgumentException) {
                    throw IllegalStateException(
                        "JWT Secret (KROLL_JWT_SECRET) must be a valid Base64-encoded string",
                        e,
                    )
                }

            if (keyBytes.size < MIN_KEY_SIZE_BYTES) {
                throw IllegalStateException("JWT Secret must be at least 256 bits ($MIN_KEY_SIZE_BYTES bytes) long.")
            }

            Keys.hmacShaKeyFor(keyBytes)
        }

    companion object {
        private const val MIN_KEY_SIZE_BYTES = 32 // 256 bits
    }
}

package com.collederas.kroll.security.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@ConfigurationProperties(prefix = "auth.jwt")
data class JwtProperties
@ConstructorBinding
constructor(
    val secret: String,
    val expiration: Duration = Duration.ofHours(1),
)

@Service
class JwtTokenService(private val properties: JwtProperties) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val jwtSigningKey =
        Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(properties.secret),
        )

    /**
     * Generate a new JWT Token with username as optional claim.
     * (username can vary at user discretion, UUID is immutable and
     * more suited for `sub` claim).
     * @return String the JWT
     */
    fun generateToken(
        userId: UUID,
        username: String,
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + properties.expiration.toMillis())
        val signingKey = jwtSigningKey

        val token =
            Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact()
        return token
    }

    /**
     * Parse a JWT token and validate its signature.
     * @return UUID the user UUID
     */
    fun validateAndGetUserId(token: String): UUID? {
        return try {
            if (token.isBlank()) return null

            val claims =
                Jwts.parser()
                    .verifyWith(jwtSigningKey)
                    .build()
                    .parseSignedClaims(token)
                    .payload

            UUID.fromString(claims.subject)
        } catch (e: JwtException) {
            logger.debug("Invalid JWT: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

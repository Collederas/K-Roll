package com.collederas.kroll.security.jwt

import com.collederas.kroll.user.AppUser
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*

@ConfigurationProperties(prefix = "auth.refresh")
data class RefreshTokenProperties(
    val expiration: Duration = DEFAULT_EXPIRATION,
    val tokenBytes: Int = DEFAULT_TOKEN_BYTES,
) {
    companion object {
        val DEFAULT_EXPIRATION: Duration = Duration.ofDays(30)
        const val DEFAULT_TOKEN_BYTES: Int = 32
    }
}

@Service
class RefreshTokenService(
    private val repository: RefreshTokenRepository,
    private val properties: RefreshTokenProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    private fun generateSecureToken(): String {
        val buffer = ByteArray(properties.tokenBytes)
        SecureRandom().nextBytes(buffer)
        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(buffer)
    }

    fun issueTokenFor(user: AppUser): String {
        val now = Instant.now(clock)
        val tokenValue = generateSecureToken()

        val token =
            RefreshTokenEntity(
                owner = user,
                token = tokenValue,
                expiresAt = now.plus(properties.expiration),
            )

        repository.save(token)
        return tokenValue
    }

    /**
     * Only one refresh token per user.
     * Rotation = revoke all existing tokens, then issue a new one.
     */
    @Transactional
    fun rotateAllTokensFor(user: AppUser): String {
        revokeTokensFor(user)
        return issueTokenFor(user)
    }

    @Transactional
    fun rotateFromRefresh(oldRefreshToken: String): Pair<AppUser, String> {
        val user = consumeToken(oldRefreshToken)
        return user to issueTokenFor(user)
    }

    @Transactional
    fun revokeTokensFor(user: AppUser) {
        repository.deleteAllByOwner(user)
    }

    @Transactional
    fun consumeToken(tokenString: String): AppUser {
        val token =
            repository.findByToken(tokenString)
                ?: throw IllegalArgumentException("Invalid refresh token")

        require(!token.expiresAt.isBefore(Instant.now(clock))) {
            "Refresh token expired"
        }

        repository.delete(token)
        return token.owner
    }
}

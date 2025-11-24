package com.collederas.kroll.security.jwt

import com.collederas.kroll.user.AppUser
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

@ConfigurationProperties(prefix = "auth.refresh")
data class RefreshTokenProperties(
    val expiration: Duration = Duration.ofDays(30),
)

@Service
class RefreshTokenService(
    private val repository: RefreshTokenRepository,
    private val properties: RefreshTokenProperties,
) {
    private fun generateSecureToken(bytes: Int = 32): String {
        val buffer = ByteArray(bytes)
        SecureRandom().nextBytes(buffer)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer)
    }

    fun issueTokenFor(user: AppUser): String {
        val now = Instant.now()
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
     * Current behavior: only ONE refresh token can exist per user.
     * Rotate = revoke all old tokens + issue one new.
     */
    @Transactional
    fun rotateAllTokensFor(user: AppUser): String {
        revokeTokensFor(user)
        return issueTokenFor(user)
    }

    @Transactional
    fun rotateFromRefresh(oldRefreshToken: String): Pair<AppUser, String> {
        val user = consumeToken(oldRefreshToken)
        val newToken = issueTokenFor(user)
        return user to newToken
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

        repository.delete(token)

        if (token.expiresAt.isBefore(Instant.now())) {
            throw IllegalArgumentException("Refresh token expired")
        }

        return token.owner
    }
}

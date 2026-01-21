package com.collederas.kroll.security.apikey

import com.collederas.kroll.core.environment.EnvironmentRepository
import com.collederas.kroll.core.exceptions.EnvironmentNotFoundException
import com.collederas.kroll.core.exceptions.InvalidApiKeyExpiryException
import com.collederas.kroll.security.apikey.dto.ApiKeyAuthResult
import com.collederas.kroll.security.apikey.dto.ApiKeyMetadataDto
import com.collederas.kroll.security.apikey.dto.CreateApiKeyRequest
import com.collederas.kroll.security.apikey.dto.CreateApiKeyResponseDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.*

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
    private val environmentRepository: EnvironmentRepository,
    private val properties: ApiKeyConfigProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    companion object {
        private const val API_KEY_BYTE_LENGTH = 24
        private const val API_KEY_PREFIX = "rk_"
        private const val API_KEY_MASK_PREFIX_LENGTH = 7
        private const val API_KEY_MASK_SUFFIX_LENGTH = 4
        private const val API_KEY_MASK_ELLIPSIS = "..."
    }

    private val secureRandom = SecureRandom()

    private fun generateSecureKey(): String {
        val bytes = ByteArray(API_KEY_BYTE_LENGTH)
        secureRandom.nextBytes(bytes)
        // Creates a string like "rk_AbCd123..."
        return API_KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun list(envId: UUID): List<ApiKeyMetadataDto> {
        if (!environmentRepository.existsById(envId)) {
            throw EnvironmentNotFoundException("Environment with ID $envId not found")
        }
        return apiKeyRepository.findAllByEnvironmentId(envId).map { it.toDto() }
    }

    @Transactional
    fun create(
        envId: UUID,
        dto: CreateApiKeyRequest,
    ): CreateApiKeyResponseDto {
        val environment =
            environmentRepository.findByIdOrNull(envId)
                ?: throw EnvironmentNotFoundException("Environment with ID $envId not found")

        val now = clock.instant()
        val finalExpiresAt: Instant? = resolveExpiryDate(dto, now)

        val rawKey = generateSecureKey()
        val hashedKey = ApiKeyHasher.hash(rawKey)

        val displayMask =
            rawKey.take(API_KEY_MASK_PREFIX_LENGTH) +
                API_KEY_MASK_ELLIPSIS +
                rawKey.takeLast(API_KEY_MASK_SUFFIX_LENGTH)

        val entity =
            ApiKeyEntity(
                environment = environment,
                keyHash = hashedKey,
                mask = displayMask,
                expiresAt = finalExpiresAt,
            )

        val apiKey = apiKeyRepository.save(entity)
        return CreateApiKeyResponseDto(
            id = apiKey.id,
            key = rawKey,
            expiresAt = apiKey.expiresAt,
            neverExpires = apiKey.expiresAt == null,
        )
    }

    @Transactional
    fun delete(apiKeyId: UUID) {
        apiKeyRepository.deleteById(apiKeyId)
    }

    private fun ApiKeyEntity.toDto() =
        ApiKeyMetadataDto(
            id = id,
            truncated = this.mask,
            environmentId = environment.id,
            createdAt = createdAt,
            expiresAt = expiresAt,
            neverExpires = expiresAt == null,
            isActive = isActive(Instant.now()),
        )

    private fun resolveExpiryDate(
        dto: CreateApiKeyRequest,
        now: Instant,
    ): Instant? =
        when {
            // Explicitly chosen by user to never expire
            dto.neverExpires -> null

            // User requests specific expiry date
            dto.expiresAt != null -> {
                validateExpiryTime(dto.expiresAt, now)
                dto.expiresAt
            }

            // User doesn't send expiration, fallback to conservative default
            else -> now.plus(properties.defaultLifetime)
        }

    private fun validateExpiryTime(
        expiresAt: Instant,
        now: Instant,
    ) {
        val maxAllowedExpiry = now.plus(properties.maxLifetime)

        if (!expiresAt.isAfter(now)) {
            throw InvalidApiKeyExpiryException("Expiration date must be in the future")
        }

        if (expiresAt.isAfter(maxAllowedExpiry)) {
            throw InvalidApiKeyExpiryException(
                "Expiration date cannot exceed the maximum allowed lifetime of ${properties.maxLifetime}",
            )
        }
    }

    // TODO: this should go in its own service
    fun validate(rawApiKey: String): ApiKeyAuthResult =
        apiKeyRepository.findByKeyHash(ApiKeyHasher.hash(rawApiKey))?.let { entity ->
            if (!entity.isActive(clock.instant())) {
                ApiKeyAuthResult.Expired
            } else {
                ApiKeyAuthResult.Valid(
                    entity.environment.id,
                    entity.id,
                    roles = listOf("ROLE_GAME_CLIENT"),
                )
            }
        } ?: ApiKeyAuthResult.Invalid
}

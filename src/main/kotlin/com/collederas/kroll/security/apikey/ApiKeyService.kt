package com.collederas.kroll.security.apikey

import com.collederas.kroll.core.environment.EnvironmentRepository
import com.collederas.kroll.core.exceptions.EnvironmentNotFoundException
import com.collederas.kroll.security.apikey.dto.ApiKeyAuthResult
import com.collederas.kroll.security.apikey.dto.ApiKeyMetadataDto
import com.collederas.kroll.security.apikey.dto.CreateApiKeyResponseDto
import com.collederas.kroll.security.apikey.exception.InvalidApiKeyExpiryException
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
        expiresAt: Instant,
    ): CreateApiKeyResponseDto {
        val environment =
            environmentRepository.findByIdOrNull(envId)
                ?: throw EnvironmentNotFoundException("Environment with ID $envId not found")

        validateExpiryTime(expiresAt, clock.instant())

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
                expiresAt = expiresAt,
            )

        val apiKey = apiKeyRepository.save(entity)
        return CreateApiKeyResponseDto(id = apiKey.id, key = rawKey, expiresAt = expiresAt)
    }

    private fun validateExpiryTime(
        expiresAt: Instant,
        now: Instant,
    ) {
        val maxAllowedExpiry = now.plus(properties.maxLifetime)

        when {
            !expiresAt.isAfter(now) ->
                throw InvalidApiKeyExpiryException("expiresAt must be in the future")

            expiresAt.isAfter(maxAllowedExpiry) ->
                throw InvalidApiKeyExpiryException(
                    "expiresAt must be within ${properties.maxLifetime}",
                )
        }
    }

    // TODO: this should go in its own service
    fun validate(rawApiKey: String): ApiKeyAuthResult {
        val hashedKey = ApiKeyHasher.hash(rawApiKey)

        return apiKeyRepository
            .findByKeyHash(hashedKey)
            ?.takeIf { it.isActive() }
            ?.let { entity ->
                ApiKeyAuthResult(
                    entity.environment.id,
                    entity.id,
                    roles = listOf("ROLE_GAME_CLIENT"),
                )
            } ?: ApiKeyAuthResult.invalid()
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
        )
}

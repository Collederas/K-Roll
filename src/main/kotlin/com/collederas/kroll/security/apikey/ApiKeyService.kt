package com.collederas.kroll.security.apikey

import com.collederas.kroll.remoteconfig.auth.ApiKeyHelper
import com.collederas.kroll.security.apikey.dto.ApiKeyMetadataDto
import com.collederas.kroll.security.apikey.dto.CreateApiKeyResponseDto
import com.collederas.kroll.remoteconfig.environment.EnvironmentRepository
import com.collederas.kroll.remoteconfig.exceptions.ApiKeyNotFoundException
import com.collederas.kroll.remoteconfig.exceptions.EnvironmentNotFoundException
import com.collederas.kroll.security.apikey.dto.ApiKeyAuthResult
import com.collederas.kroll.security.apikey.exception.InvalidApiKeyExpiryException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
    private val environmentRepository: EnvironmentRepository,
    private val properties: ApiKeyConfig,
    private val clock: Clock = Clock.systemUTC()
) {
    private val secureRandom = SecureRandom()

    private fun generateSecureKey(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        // Creates a string like "rk_AbCd123..."
        return "rk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun list(envId: UUID): List<ApiKeyMetadataDto> {
        if (!environmentRepository.existsById(envId)) {
            throw EnvironmentNotFoundException("Environment with ID $envId not found")
        }
        return apiKeyRepository.findAllByEnvironmentId(envId).map { it.toDto() }
    }

    @Transactional
    fun create(envId: UUID, expiresAt: Instant): CreateApiKeyResponseDto {
        val environment = environmentRepository.findByIdOrNull(envId)
            ?: throw EnvironmentNotFoundException("Environment with ID $envId not found")

        val now = Instant.now(clock)

        if (!expiresAt.isAfter(now)) {
            throw InvalidApiKeyExpiryException("expiresAt must be in the future")
        }
        if (expiresAt.isAfter(now.plus(properties.maxLifetime))) {
            throw InvalidApiKeyExpiryException("expiresAt must be within ${properties.maxLifetime} days")
        }

        val rawKey = generateSecureKey()
        val hashedKey = ApiKeyHelper.hash(rawKey)

        // We take the first 4 chars + ellipses + last 4 chars for better identification
        val displayMask = "${rawKey.take(7)}...${rawKey.takeLast(4)}"

        val entity = ApiKeyEntity(
            environment = environment,
            keyHash = hashedKey,
            mask = displayMask,
            expiresAt = expiresAt,
        )
        val apiKey = apiKeyRepository.save(entity)
        return CreateApiKeyResponseDto(id = apiKey.id, key = rawKey, expiresAt = expiresAt)
    }

    fun validate(rawApiKey: String): ApiKeyAuthResult {
        val hashedKey = ApiKeyHelper.hash(rawApiKey)

        val entity = apiKeyRepository.findByKeyHash(hashedKey)
            ?: return ApiKeyAuthResult.invalid()

        if (!entity.isActive())
            return ApiKeyAuthResult.invalid()

        return ApiKeyAuthResult(
            entity.environment.id,
            entity.id,
            roles = listOf("ROLE_GAME_CLIENT")
        )
    }

    @Transactional
    fun delete(apiKeyId: UUID) {
        if (!apiKeyRepository.existsById(apiKeyId)) {
            throw ApiKeyNotFoundException("API Key with ID $apiKeyId not found")
        }
        apiKeyRepository.deleteById(apiKeyId)
    }

    private fun ApiKeyEntity.toDto() = ApiKeyMetadataDto(
        id = id,
        truncated = this.mask,
        environmentId = environment.id,
        createdAt = createdAt,
    )
}

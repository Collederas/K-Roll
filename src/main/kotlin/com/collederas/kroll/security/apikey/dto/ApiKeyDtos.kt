package com.collederas.kroll.security.apikey.dto

import java.time.Instant
import java.util.*

data class CreateApiKeyRequest(
    val expiresAt: Instant,
)

data class CreateApiKeyResponseDto(
    val id: UUID,
    val key: String,
    val expiresAt: Instant,
)

data class ApiKeyMetadataDto(
    val id: UUID,
    val truncated: String, // we don't want to return full key
    val environmentId: UUID,
    val createdAt: Instant,
)

sealed interface ApiKeyAuthResult {
    data class Valid(
        val environmentId: UUID,
        val apiKeyId: UUID,
        val roles: List<String>,
    ) : ApiKeyAuthResult

    data object Invalid : ApiKeyAuthResult

    data object Expired : ApiKeyAuthResult
}

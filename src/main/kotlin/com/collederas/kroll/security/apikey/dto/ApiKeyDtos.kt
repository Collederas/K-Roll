package com.collederas.kroll.security.apikey.dto

import java.time.Instant
import java.util.*

data class CreateApiKeyRequest(
    val expiresAt: Instant? = null,
    val neverExpires: Boolean = false,
)

data class CreateApiKeyResponseDto(
    val id: UUID,
    val key: String,
    val expiresAt: Instant?,
    val neverExpires: Boolean,
)

data class ApiKeyMetadataDto(
    val id: UUID,
    val truncated: String,
    val environmentId: UUID,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val neverExpires: Boolean,
    val isActive: Boolean, // not expired
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

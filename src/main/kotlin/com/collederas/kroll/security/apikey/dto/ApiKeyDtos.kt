package com.collederas.kroll.security.apikey.dto

import java.time.Instant
import java.util.UUID

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

data class ApiKeyAuthResult(
    val environmentId: UUID?,
    val apiKeyId: UUID?,
    val roles: List<String>,
) {
    companion object {
        fun invalid() = ApiKeyAuthResult(null, null, emptyList())
    }
}

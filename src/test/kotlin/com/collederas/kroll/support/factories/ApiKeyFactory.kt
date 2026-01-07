package com.collederas.kroll.support.factories

import com.collederas.kroll.security.apikey.ApiKeyEntity
import java.time.Instant
import java.util.*

// TODO: make test only @Profile("test")
object ApiKeyFactory {
    fun create(
        id: UUID = UUID.randomUUID(),
        keyHash: String = "default_hashed_value",
        mask: String = "rk_...test",
        expiresAt: Instant = Instant.now().plusSeconds(3600),
    ): ApiKeyEntity {
        return ApiKeyEntity(
            id = id,
            environment = EnvironmentFactory.create(),
            keyHash = keyHash,
            mask = mask,
            expiresAt = expiresAt,
        )
    }
}

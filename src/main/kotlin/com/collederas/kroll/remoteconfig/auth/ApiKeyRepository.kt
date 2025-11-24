package com.collederas.kroll.remoteconfig.auth

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ApiKeyRepository : JpaRepository<ApiKeyEntity, UUID> {
    fun findAllByEnvironmentId(environmentId: UUID): List<ApiKeyEntity>

    fun findByApiKey(apiKey: String): ApiKeyEntity?
}

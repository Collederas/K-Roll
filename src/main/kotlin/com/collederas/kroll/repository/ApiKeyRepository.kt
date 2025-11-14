package com.collederas.kroll.repository

import com.collederas.kroll.entity.ApiKeyEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApiKeyRepository : JpaRepository<ApiKeyEntity, UUID> {
    fun findAllByEnvironmentId(environmentId: UUID): List<ApiKeyEntity>
    fun findByApiKey(apiKey: String): ApiKeyEntity?
}
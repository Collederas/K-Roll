package com.collederas.kroll.security.apikey

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApiKeyRepository : JpaRepository<ApiKeyEntity, UUID> {
    fun findAllByEnvironmentId(environmentId: UUID): List<ApiKeyEntity>

    fun findByKeyHash(keyHash: String): ApiKeyEntity?
}

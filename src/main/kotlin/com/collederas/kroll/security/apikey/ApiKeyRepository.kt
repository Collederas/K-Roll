package com.collederas.kroll.security.apikey

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface ApiKeyRepository : JpaRepository<ApiKeyEntity, UUID> {
    fun findAllByEnvironmentId(environmentId: UUID): List<ApiKeyEntity>

    fun findByKeyHash(keyHash: String): ApiKeyEntity?

    @Query(
        """
        select count(k) > 0
        from ApiKeyEntity k
        where k.environment.id = :environmentId
          and (k.expiresAt is null or k.expiresAt > :now)
        """,
    )
    fun existsActiveKeyForEnvironment(
        environmentId: UUID,
        now: Instant,
    ): Boolean
}

package com.collederas.kroll.security.apikey

import com.collederas.kroll.security.apikey.dto.ApiKeyMetadataDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface ApiKeyRepository : JpaRepository<ApiKeyEntity, UUID> {
    fun findAllByEnvironmentId(environmentId: UUID): List<ApiKeyEntity>

    @Query(
        """
        SELECT new com.collederas.kroll.security.apikey.dto.ApiKeyMetadataDto(
            k.id,
            k.mask,
            k.environment.id,
            k.createdAt,
            k.expiresAt,
            CASE WHEN k.expiresAt IS NULL THEN true ELSE false END,
            CASE WHEN k.expiresAt IS NULL OR k.expiresAt > :now THEN true ELSE false END
        )
        FROM ApiKeyEntity k
        WHERE k.environment.id = :environmentId
        """,
    )
    fun findAllDtosByEnvironmentId(
        environmentId: UUID,
        now: Instant,
    ): List<ApiKeyMetadataDto>

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

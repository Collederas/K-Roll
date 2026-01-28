package com.collederas.kroll.core.configentry.versioning

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConfigVersionRepository : JpaRepository<ConfigVersionEntity, UUID> {

    fun findTopByEnvironmentIdOrderByCreatedAtDesc(
        environmentId: UUID
    ): ConfigVersionEntity?

    @Query(
        """
        SELECT v.version
        FROM ConfigVersionEntity v
        WHERE v.environmentId = :environmentId
        ORDER BY v.createdAt desc
        LIMIT 1
        """
    )
    fun findLatestVersionString(
        @Param("environmentId") environmentId: UUID
    ): String?

    /**
     * Allocates the next version identifier for an environment.
     *
     * This must be called inside a transaction that also inserts the new version,
     * relying on the (environment_id, version) unique constraint for safety.
     */
    fun nextVersionForEnvironment(environmentId: UUID): String {
        val latest = findLatestVersionString(environmentId) ?: return "v1"
        val numeric = latest.removePrefix("v").toInt()
        return "v${numeric + 1}"
    }
}

package com.collederas.kroll.core.configentry.versioning.snapshot

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConfigSnapshotRepository :
    JpaRepository<ConfigSnapshotEntity, UUID> {

    fun findByEnvironmentIdAndVersion(
        environmentId: UUID,
        version: String,
    ): ConfigSnapshotEntity?

    fun existsByEnvironmentIdAndVersion(
        environmentId: UUID,
        version: String,
    ): Boolean

    @Query(
        """
        SELECT s
        FROM ConfigSnapshotEntity s
        WHERE s.environmentId = :environmentId
        ORDER by s.createdAt desc
        """
    )
    fun findLatestSnapshot(
        @Param("environmentId") environmentId: UUID,
    ): List<ConfigSnapshotEntity>
}

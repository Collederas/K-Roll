package com.collederas.kroll.core.config.versioning.snapshot

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConfigSnapshotRepository : JpaRepository<ConfigSnapshotEntity, UUID> {
    fun findByVersionId(versionId: UUID): ConfigSnapshotEntity?
}

package com.collederas.kroll.core.config.versioning

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

// ASSUMES THAT AN active_versions ROW EXIST PER ENVIRONMENT
@Repository
interface ActiveVersionRepository : JpaRepository<ActiveVersionEntity, UUID> {
    fun findByEnvironmentId(environmentId: UUID): ActiveVersionEntity

    @Query("SELECT a from ActiveVersionEntity a WHERE a.environmentId = :envId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findLocked(
        @Param("envId") envId: UUID,
    ): ActiveVersionEntity
}

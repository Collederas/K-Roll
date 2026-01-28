package com.collederas.kroll.core.configentry.versioning

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ActiveVersionRepository :
    JpaRepository<ActiveVersionEntity, UUID> {

    fun findByEnvironmentId(environmentId: UUID): ActiveVersionEntity?
}

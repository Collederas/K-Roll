package com.collederas.kroll.core.environment

import com.collederas.kroll.core.exceptions.ForbiddenException
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnvironmentAccessGuard(
    private val environmentRepository: EnvironmentRepository
) {
    fun requireOwner(envId: UUID, userId: UUID) {
        if (!environmentRepository.existsByIdAndProjectOwnerId(envId, userId)) {
            throw ForbiddenException("Environment not owned by user")
        }
    }
}

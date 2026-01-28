package com.collederas.kroll.core.environment

import com.collederas.kroll.exceptions.EnvironmentNotFoundException
import com.collederas.kroll.exceptions.ForbiddenException
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnvironmentAccessGuard(
    private val environmentRepository: EnvironmentRepository,
) {
    fun requireOwner(
        envId: UUID,
        userId: UUID,
    ) {
        if (!environmentRepository.existsByIdAndProjectOwnerId(envId, userId)) {
            if (!environmentRepository.existsById(envId)) {
                throw EnvironmentNotFoundException("Environment not found")
            }
            throw ForbiddenException("Project not owned by user")
        }
    }
}

package com.collederas.kroll.core.environment

import com.collederas.kroll.core.exceptions.EnvironmentNotFoundException
import com.collederas.kroll.core.exceptions.ForbiddenException
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
        val environment =
            environmentRepository
                .findById(envId)
                .orElseThrow { EnvironmentNotFoundException("Environment not found") }

        if (environment.project.owner.id != userId) {
            throw ForbiddenException("Project not owned by user")
        }
    }
}

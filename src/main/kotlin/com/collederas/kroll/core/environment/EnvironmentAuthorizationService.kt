package com.collederas.kroll.core.environment

import com.collederas.kroll.exceptions.EnvironmentNotFoundException
import com.collederas.kroll.exceptions.ForbiddenException
import org.springframework.stereotype.Component
import java.util.*

@Component
class EnvironmentAuthorizationService(
    private val environmentRepository: EnvironmentRepository,
) {
    fun isOwner(envId: UUID, userId: UUID): Boolean =
        environmentRepository.existsByIdAndProjectOwnerId(envId, userId)
}

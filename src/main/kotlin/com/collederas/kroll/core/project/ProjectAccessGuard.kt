package com.collederas.kroll.core.project

import com.collederas.kroll.core.exceptions.ForbiddenException
import org.springframework.stereotype.Component
import java.util.*

@Component
class ProjectAccessGuard(
    private val projectRepository: ProjectRepository
) {

    fun requireOwner(projectId: UUID, userId: UUID) {
        if (!projectRepository.existsByIdAndOwnerId(projectId, userId)) {
            throw ForbiddenException("Project not owned by user")
        }
    }
}


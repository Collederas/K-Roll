package com.collederas.kroll.core.environment

import com.collederas.kroll.core.environment.dto.CreateEnvironmentDto
import com.collederas.kroll.core.environment.dto.EnvironmentResponseDto
import com.collederas.kroll.core.exceptions.ProjectNotFoundException
import com.collederas.kroll.core.project.ProjectAccessGuard
import com.collederas.kroll.core.project.ProjectRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class EnvironmentService(
    private val projectAccessGuard: ProjectAccessGuard,
    private val projectRepository: ProjectRepository,
    private val environmentRepository: EnvironmentRepository,
) {
    fun list(
        projectId: UUID,
        userId: UUID,
    ): List<EnvironmentResponseDto> {
        projectAccessGuard.requireOwner(projectId, userId)

        if (!projectRepository.existsById(projectId)) {
            throw ProjectNotFoundException("Project with ID $projectId not found")
        }
        return environmentRepository.findAllByProjectId(projectId).map {
            EnvironmentResponseDto(it.id, it.name, it.project.id)
        }
    }

    @Transactional
    fun create(
        userId: UUID,
        projectId: UUID,
        dto: CreateEnvironmentDto,
    ): EnvironmentResponseDto {
        projectAccessGuard.requireOwner(projectId, userId)
        val project =
            projectRepository.findByIdOrNull(projectId)
                ?: throw ProjectNotFoundException("Project with ID $projectId not found")

        val environment =
            EnvironmentEntity(
                project = project,
                name = dto.name,
            )
        val savedEnv = environmentRepository.save(environment)

        return EnvironmentResponseDto(savedEnv.id, savedEnv.name, savedEnv.project.id)
    }
}

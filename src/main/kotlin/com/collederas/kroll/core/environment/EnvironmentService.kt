package com.collederas.kroll.core.environment

import com.collederas.kroll.core.environment.dto.CreateEnvironmentDto
import com.collederas.kroll.core.environment.dto.EnvironmentResponseDto
import com.collederas.kroll.core.exceptions.EnvironmentAlreadyExistsException
import com.collederas.kroll.core.exceptions.EnvironmentNotFoundException
import com.collederas.kroll.core.project.ProjectService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class EnvironmentService(
    private val projectService: ProjectService,
    private val environmentRepository: EnvironmentRepository,
) {
    @Transactional(readOnly = true)
    fun list(
        projectId: UUID?,
        userId: UUID,
    ): List<EnvironmentResponseDto> {
        if (projectId != null) {
            projectService.resolveOwnedProject(projectId, userId)

            return environmentRepository
                .findAllByProjectId(projectId)
                .map {
                    EnvironmentResponseDto(it.id, it.name, it.project.id)
                }
        }

        return environmentRepository
            .findAllByProjectOwnerId(userId)
            .map {
                EnvironmentResponseDto(it.id, it.name, it.project.id)
            }
    }

    @Transactional(readOnly = true)
    fun get(
        envId: UUID,
        userId: UUID,
    ): EnvironmentResponseDto {
        val environment =
            environmentRepository
                .findById(envId)
                .orElseThrow {
                    EnvironmentNotFoundException("Environment not found")
                }

        projectService.resolveOwnedProject(environment.project.id, userId)

        return EnvironmentResponseDto(
            environment.id,
            environment.name,
            environment.project.id,
        )
    }

    @Transactional
    fun create(
        userId: UUID,
        dto: CreateEnvironmentDto,
    ): EnvironmentResponseDto {
        val project =
            projectService.resolveOwnedProject(dto.projectId, userId)

        if (environmentRepository.existsByProjectIdAndName(project.id, dto.name)) {
            throw EnvironmentAlreadyExistsException(
                "Environment with name '${dto.name}' already exists in this project.",
            )
        }

        val environment =
            EnvironmentEntity(
                project = project,
                name = dto.name,
            )
        val savedEnv = environmentRepository.save(environment)

        return EnvironmentResponseDto(savedEnv.id, savedEnv.name, savedEnv.project.id)
    }
}

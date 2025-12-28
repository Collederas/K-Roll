package com.collederas.kroll.support.factories

import com.collederas.kroll.remoteconfig.environment.EnvironmentEntity
import com.collederas.kroll.remoteconfig.environment.EnvironmentRepository
import com.collederas.kroll.remoteconfig.project.ProjectEntity
import com.collederas.kroll.remoteconfig.project.ProjectRepository
import com.collederas.kroll.user.AppUserRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

object EnvironmentFactory {
    fun create(
        project: ProjectEntity = ProjectFactory.create(),

        ): EnvironmentEntity {
        return EnvironmentEntity(
            id = UUID.randomUUID(),
            project = project,
            name = "Test Environment",
        )
    }
}

@Component
@Profile("test")
class PersistedEnvironmentFactory(
    private val userRepo: AppUserRepository,
    private val projectRepo: ProjectRepository,
    private val environmentRepo: EnvironmentRepository,
) {
    fun create(): EnvironmentEntity {
        val user = userRepo.save(UserFactory.create())
        val project = projectRepo.save(ProjectFactory.create(owner = user))
        return environmentRepo.save(EnvironmentFactory.create(project = project))
    }
}


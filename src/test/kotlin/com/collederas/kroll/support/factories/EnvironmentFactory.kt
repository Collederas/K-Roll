package com.collederas.kroll.support.factories

import com.collederas.kroll.core.environment.EnvironmentEntity
import com.collederas.kroll.core.environment.EnvironmentRepository
import com.collederas.kroll.core.project.ProjectEntity
import com.collederas.kroll.core.project.ProjectRepository
import com.collederas.kroll.user.AppUser
import com.collederas.kroll.user.AppUserRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

// TODO: make test only too
object EnvironmentFactory {
    fun create(
        project: ProjectEntity = ProjectFactory.create(),
        name: String = "Test Environment"
    ): EnvironmentEntity {
        return EnvironmentEntity(
            id = UUID.randomUUID(),
            project = project,
            name = name,
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
    @Transactional
    fun create(user: AppUser? = null, name: String = "Test Environment"): EnvironmentEntity {
        var owner = user
        if (owner == null) {
            owner = userRepo.save(UserFactory.create())
        }
        val project = projectRepo.save(ProjectFactory.create(owner = owner))
        return environmentRepo.save(EnvironmentFactory.create(project = project, name = name))
    }
}

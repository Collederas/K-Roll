package com.collederas.kroll.support.factories

import com.collederas.kroll.core.project.ProjectEntity
import com.collederas.kroll.user.AppUser
import java.util.*

// TODO: make test only @Profile("test")
object ProjectFactory {
    fun create(owner: AppUser = UserFactory.create()): ProjectEntity {
        return ProjectEntity(
            id = UUID.randomUUID(),
            name = "Test Project",
            owner = owner,
        )
    }
}

package com.collederas.kroll.remoteconfig.project

import com.collederas.kroll.remoteconfig.project.dto.CreateProjectDto
import com.collederas.kroll.support.factories.UserFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class ProjectServiceTests {
    private val repo: ProjectRepository = mockk(relaxed = true)
    private val projectService = ProjectService(repo)

    @Test
    fun `should create project successfully`() {
        val owner = UserFactory.create()

        val createDto = CreateProjectDto("My New Project")

        every { repo.save(any()) } returnsArgument 0

        val result = projectService.create(owner, createDto)

        assertEquals("My New Project", result.name)
        verify(exactly = 1) { repo.save(any()) }
    }

    @Test
    fun `should delete project successfully`() {
        val projectId = UUID.randomUUID()

        projectService.delete(projectId)

        verify(exactly = 1) { repo.deleteById(projectId) }
    }
}

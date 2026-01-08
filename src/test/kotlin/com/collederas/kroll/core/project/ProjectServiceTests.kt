package com.collederas.kroll.core.project

import com.collederas.kroll.core.project.dto.CreateProjectDto
import com.collederas.kroll.support.factories.UserFactory
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class ProjectServiceTests {
    private val repo: ProjectRepository = mockk(relaxed = true)
    private val projectAccessGuard: ProjectAccessGuard = mockk()
    private val projectService = ProjectService(repo, projectAccessGuard)

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

        every {
            projectAccessGuard.requireOwner(any(), any())
        } just Runs

        projectService.delete(UUID.randomUUID(), projectId)

        verify(exactly = 1) { repo.deleteById(projectId) }
    }
}

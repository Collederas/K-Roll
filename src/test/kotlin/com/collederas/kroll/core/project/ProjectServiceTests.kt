package com.collederas.kroll.core.project

import com.collederas.kroll.core.project.dto.CreateProjectDto
import com.collederas.kroll.support.factories.UserFactory
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class ProjectServiceTests {
    private val repo: ProjectRepository = mockk(relaxed = true)
    private val projectService = ProjectService(repo)

    @Test
    fun `should create project successfully`() {
        val owner = UserFactory.create()
        val createDto = CreateProjectDto("My New Project")

        every { repo.existsByOwnerIdAndName(owner.id, createDto.name) } returns false
        every { repo.save(any()) } returnsArgument 0

        val result = projectService.create(owner, createDto)

        assertEquals("My New Project", result.name)
        verify(exactly = 1) { repo.save(any()) }
    }

    @Test
    fun `should delete project successfully`() {
        val owner = UserFactory.create()
        val projectId = UUID.randomUUID()

        val slot = slot<ProjectEntity>()

        val project =
            ProjectEntity(
                id = projectId,
                name = "Test Project",
                owner = owner,
            )

        every { repo.findById(projectId) } returns Optional.of(project)
        every { repo.delete(project) } just Runs

        projectService.delete(owner.id, projectId)

        verify(exactly = 1) { repo.delete(capture(slot)) }
    }
}

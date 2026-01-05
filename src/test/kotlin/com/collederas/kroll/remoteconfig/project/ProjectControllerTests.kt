package com.collederas.kroll.remoteconfig.project

import com.collederas.kroll.core.project.ProjectService
import com.collederas.kroll.core.project.dto.CreateProjectDto
import com.collederas.kroll.core.project.dto.ProjectDto
import com.collederas.kroll.support.factories.AuthUserFactory
import com.collederas.kroll.support.factories.UserFactory
import com.collederas.kroll.user.UserRole
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerTests {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var projectService: ProjectService

    @Test
    fun `should return 200 and list of projects`() {
        val authUser = AuthUserFactory.create(UserFactory.create(roles = setOf(UserRole.ADMIN)))
        val projects = listOf(ProjectDto(UUID.randomUUID(), "Project 1"))
        every { projectService.list() } returns projects

        mvc.perform(get("/admin/projects")
            .with(user(authUser))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().json(objectMapper.writeValueAsString(projects)))

        verify(exactly = 1) { projectService.list() }
    }

    @Test
    fun `create should return created project`() {
        val authUser = AuthUserFactory.create(UserFactory.create(roles = setOf(UserRole.ADMIN)))
        val createDto = CreateProjectDto("New Project")
        val projectDto = ProjectDto(UUID.randomUUID(), "New Project")

        every { projectService.create(any(), any()) } returns projectDto

        mvc.perform(
            post("/admin/projects")
                .with(user(authUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)),
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().json(objectMapper.writeValueAsString(projectDto)))

        verify(exactly = 1) { projectService.create(any(), createDto) }
    }

    @Test
    fun `delete should remove project`() {
        val id = UUID.randomUUID()
        every { projectService.delete(id) } returns Unit

        mvc.perform(
            delete("/admin/projects/{id}", id)
                .with(user("admin").roles("ADMIN")),
        )
            .andExpect(status().isOk)

        verify(exactly = 1) { projectService.delete(id) }
    }
}

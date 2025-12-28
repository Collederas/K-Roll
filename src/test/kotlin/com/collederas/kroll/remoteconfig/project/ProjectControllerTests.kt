package com.collederas.kroll.remoteconfig.project

import com.collederas.kroll.remoteconfig.project.dto.CreateProjectDto
import com.collederas.kroll.remoteconfig.project.dto.ProjectDto
import com.collederas.kroll.security.user.AuthUserDetailsService
import com.collederas.kroll.security.SecurityConfig
import com.collederas.kroll.security.jwt.JwtAuthEntryPoint
import com.collederas.kroll.security.jwt.JwtAuthFilter
import com.collederas.kroll.security.jwt.JwtTokenService
import com.collederas.kroll.support.factories.AuthUserFactory
import com.collederas.kroll.support.factories.UserFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(ProjectController::class)
@Import(SecurityConfig::class, JwtAuthFilter::class)
class ProjectControllerTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var projectService: ProjectService

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var userDetailsService: AuthUserDetailsService

    @MockkBean(relaxed = true)
    private lateinit var jwtAuthEntryPoint: JwtAuthEntryPoint

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `list should return all projects`() {
        val projects =
            listOf(
                ProjectDto(UUID.randomUUID(), "Project 1"),
                ProjectDto(UUID.randomUUID(), "Project 2"),
            )
        every { projectService.list() } returns projects

        mockMvc.perform(get("/admin/projects").with(user("admin").roles("ADMIN")))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().json(objectMapper.writeValueAsString(projects)))

        verify(exactly = 1) { projectService.list() }
    }

    @Test
    fun `create should return created project`() {
        val user = UserFactory.create()
        val authUser = AuthUserFactory.create(user)
        val createDto = CreateProjectDto("New Project")
        val projectDto = ProjectDto(UUID.randomUUID(), "New Project")

        every { projectService.create(any(), any()) } returns projectDto

        mockMvc.perform(
            post("/admin/projects")
                .with(user(authUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)),
        )
            .andExpect(status().isOk)
            .andExpect(content().json(objectMapper.writeValueAsString(projectDto)))

        verify(exactly = 1) { projectService.create(any(), createDto) }
    }

    @Test
    fun `delete should remove project`() {
        val id = UUID.randomUUID()
        every { projectService.delete(id) } returns Unit

        mockMvc.perform(
            delete("/admin/projects/{id}", id)
                .with(user("admin").roles("ADMIN")),
        )
            .andDo(print())
            .andExpect(status().isOk)

        verify(exactly = 1) { projectService.delete(id) }
    }
}

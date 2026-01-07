package com.collederas.kroll.remoteconfig.project

import com.collederas.kroll.core.project.ProjectService
import com.collederas.kroll.core.project.dto.CreateProjectDto
import com.collederas.kroll.support.factories.UserFactory
import com.collederas.kroll.user.AppUserRepository
import com.collederas.kroll.user.UserRole
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test", "test-persistence")
class ProjectIntegrationTests {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var projectService: ProjectService

    @Autowired
    lateinit var userRepository: AppUserRepository

    private val endpoint = "/admin/projects"

    @Test
    fun `list should return only user projects`() {
        val user = userRepository.save(
            UserFactory.create(roles = setOf(UserRole.ADMIN))
        )

        val authUser = user.asAuth()

        projectService.create(user, CreateProjectDto("Project A"))
        projectService.create(user, CreateProjectDto("Project B"))

        mvc.get(endpoint) {
            with(user(authUser))
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].name") { value("Project A") }
            jsonPath("$[1].name") { value("Project B") }
        }
    }

    @Test
    fun `create should persist and return project`() {
        val user = userRepository.save(
            UserFactory.create(roles = setOf(UserRole.ADMIN))
        )

        val authUser = user.asAuth()

        val request = CreateProjectDto("New Project")

        mvc.post(endpoint) {
            with(user(authUser))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("New Project") }
        }

        val persisted = projectService.list(user.id)
        assertThat(persisted).hasSize(1)
        assertThat(persisted.first().name).isEqualTo("New Project")
    }

    @Test
    fun `delete should remove project`() {
        val user = userRepository.save(
            UserFactory.create(roles = setOf(UserRole.ADMIN))
        )

        val authUser = user.asAuth()

        val project = projectService.create(
            user,
            CreateProjectDto("To Be Deleted")
        )

        mvc.delete("$endpoint/{id}", project.id) {
            with(user(authUser))
        }.andExpect {
            status { isOk() }
        }

        val remaining = projectService.list(user.id)
        assertThat(remaining).isEmpty()
    }

    private fun com.collederas.kroll.user.AppUser.asAuth(): UserDetails =
        com.collederas.kroll.security.identity.AuthUserDetails(this)
}

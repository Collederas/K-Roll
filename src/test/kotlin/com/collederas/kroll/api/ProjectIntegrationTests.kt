package com.collederas.kroll.api

import com.collederas.kroll.core.project.ProjectService
import com.collederas.kroll.core.project.dto.CreateProjectDto
import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.support.factories.UserFactory
import com.collederas.kroll.user.AppUser
import com.collederas.kroll.user.AppUserRepository
import com.collederas.kroll.user.UserRole
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectIntegrationTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var projectService: ProjectService

    @Autowired
    lateinit var userRepository: AppUserRepository

    private val endpoint = "/projects"

    @Test
    fun `list should return only user projects`() {
        val user =
            userRepository.save(
                UserFactory.create(roles = setOf(UserRole.ADMIN)),
            )

        val authUser = user.asAuth()

        projectService.create(user, CreateProjectDto("Project A"))
        projectService.create(user, CreateProjectDto("Project B"))

        mvc
            .get(endpoint) {
                with(SecurityMockMvcRequestPostProcessors.user(authUser))
            }.andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].name") { value("Project A") }
                jsonPath("$[1].name") { value("Project B") }
            }
    }

    @Test
    fun `create should persist and return project`() {
        val user =
            userRepository.save(
                UserFactory.create(roles = setOf(UserRole.ADMIN)),
            )

        val authUser = user.asAuth()

        val request = CreateProjectDto("New Project")

        mvc
            .post(endpoint) {
                with(SecurityMockMvcRequestPostProcessors.user(authUser))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("New Project") }
            }

        val persisted = projectService.list(user.id)
        Assertions.assertThat(persisted).hasSize(1)
        Assertions.assertThat(persisted.first().name).isEqualTo("New Project")
    }

    @Test
    fun `delete should remove project`() {
        val user =
            userRepository.save(
                UserFactory.create(roles = setOf(UserRole.ADMIN)),
            )

        val authUser = user.asAuth()

        val project =
            projectService.create(
                user,
                CreateProjectDto("To Be Deleted"),
            )

        mvc
            .delete("$endpoint/{id}", project.id) {
                with(SecurityMockMvcRequestPostProcessors.user(authUser))
            }.andExpect {
                status { isOk() }
            }

        val remaining = projectService.list(user.id)
        Assertions.assertThat(remaining).isEmpty()
    }

    @Test
    fun `create with duplicate name for same owner returns conflict but is idempotent`() {
        val user =
            userRepository.save(
                UserFactory.create(roles = setOf(UserRole.ADMIN)),
            )

        val authUser = user.asAuth()

        val request = CreateProjectDto("Duplicate Project")

        // First creation should succeed
        mvc
            .post(endpoint) {
                with(SecurityMockMvcRequestPostProcessors.user(authUser))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Duplicate Project") }
            }

        // Second creation with same name should return conflict
        mvc
            .post(endpoint) {
                with(SecurityMockMvcRequestPostProcessors.user(authUser))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isConflict() }
                jsonPath("$.title") { value("Project Already Exists") }
                jsonPath("$.error_code") { value("PROJECT_ALREADY_EXISTS") }
            }

        // Verify idempotency - still only one project exists
        val persisted = projectService.list(user.id)
        Assertions.assertThat(persisted).hasSize(1)
        Assertions.assertThat(persisted.first().name).isEqualTo("Duplicate Project")
    }

    @Test
    fun `create with same name for different owners is allowed`() {
        val user1 =
            userRepository.save(
                UserFactory.create(roles = setOf(UserRole.ADMIN)),
            )

        val user2 =
            userRepository.save(
                UserFactory.create(roles = setOf(UserRole.ADMIN)),
            )

        val authUser1 = user1.asAuth()
        val authUser2 = user2.asAuth()

        val request = CreateProjectDto("Shared Project Name")

        // First user creates project
        mvc
            .post(endpoint) {
                with(SecurityMockMvcRequestPostProcessors.user(authUser1))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Shared Project Name") }
            }

        // Second user creates project with same name - should succeed
        mvc
            .post(endpoint) {
                with(SecurityMockMvcRequestPostProcessors.user(authUser2))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.name") { value("Shared Project Name") }
            }

        // Verify both users have their own project
        val user1Projects = projectService.list(user1.id)
        val user2Projects = projectService.list(user2.id)

        Assertions.assertThat(user1Projects).hasSize(1)
        Assertions.assertThat(user1Projects.first().name).isEqualTo("Shared Project Name")

        Assertions.assertThat(user2Projects).hasSize(1)
        Assertions.assertThat(user2Projects.first().name).isEqualTo("Shared Project Name")
    }

    @Test
    fun `should return 404 when project does not exist`() {
        val user =
            userRepository.save(
                UserFactory.create(roles = setOf(UserRole.ADMIN)),
            )
        val authUser = user.asAuth()
        val nonExistentProjectId = java.util.UUID.randomUUID()

        mvc
            .delete("$endpoint/{id}", nonExistentProjectId) {
                with(SecurityMockMvcRequestPostProcessors.user(authUser))
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.title") { value("Project Not Found") }
                jsonPath("$.error_code") { value("PROJECT_NOT_FOUND") }
            }
    }

    private fun AppUser.asAuth(): UserDetails = AuthUserDetails(this)
}

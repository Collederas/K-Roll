package com.collederas.kroll.api

import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.support.TestClockConfig
import com.collederas.kroll.support.factories.UserFactory
import com.collederas.kroll.user.AppUserRepository
import com.collederas.kroll.user.UserRole
import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import com.collederas.kroll.core.environment.dto.CreateEnvironmentDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig::class)
@ActiveProfiles("test")
class EnvironmentIntegrationTests {
    @Autowired
    private lateinit var userRepository: AppUserRepository

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var persistedEnvironmentFactory: PersistedEnvironmentFactory

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should return 404 when environment does not exist`() {
        val user = userRepository.save(UserFactory.create(roles = setOf(UserRole.ADMIN)))
        val authUser = AuthUserDetails(user)
        val nonExistentEnvId = UUID.randomUUID()

        mvc.get("/api/environments/{envId}/configs", nonExistentEnvId) {
            with(SecurityMockMvcRequestPostProcessors.user(authUser))
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.title") { value("Environment Not Found") }
            jsonPath("$.error_code") { value("ENVIRONMENT_NOT_FOUND") }
        }
    }

    @Test
    fun `should return 409 Conflict when creating duplicate environment`() {
        val user = userRepository.save(UserFactory.create(roles = setOf(UserRole.ADMIN)))
        val authUser = AuthUserDetails(user)
        val existingEnv = persistedEnvironmentFactory.create(user = user)

        val dto = CreateEnvironmentDto(
            name = existingEnv.name,
            projectId = existingEnv.project.id
        )

        mvc.post("/api/environments") {
            with(SecurityMockMvcRequestPostProcessors.user(authUser))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(dto)
        }.andDo {
            print()
        }.andExpect {
            status { isConflict() }
        }
    }
}

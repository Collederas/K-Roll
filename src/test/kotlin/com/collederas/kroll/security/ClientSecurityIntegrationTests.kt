package com.collederas.kroll.security

import com.collederas.kroll.core.environment.EnvironmentEntity
import com.collederas.kroll.core.project.ProjectEntity
import com.collederas.kroll.security.apikey.ApiKeyEntity
import com.collederas.kroll.security.apikey.ApiKeyHasher
import com.collederas.kroll.security.apikey.ApiKeyRepository
import com.collederas.kroll.support.factories.UserFactory
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientSecurityIntegrationTests {
    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    lateinit var apiKeyRepository: ApiKeyRepository

    @Test
    fun `public route - client api is not accessible without token`() {
        mvc.get("/client/ping")
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `api key route - valid api key returns 200`() {
        val mockEnv =
            EnvironmentEntity(
                id = UUID.randomUUID(),
                name = "Test Env",
                project = ProjectEntity(name = "Test", owner = UserFactory.create()),
            )

        val validKey = "api_key_12345"
        val hashedKey = ApiKeyHasher.hash(validKey)

        val mockKeyEntity =
            ApiKeyEntity(
                environment = mockEnv,
                keyHash = hashedKey,
                mask = "rk_l...2345",
                expiresAt = Instant.now().plusSeconds(3600),
            )

        every { apiKeyRepository.findByKeyHash(hashedKey) } returns mockKeyEntity

        mvc.get("/client/ping") {
            header("X-Api-Key", validKey)
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `api key route - invalid api key returns 401`() {
        val key = ApiKeyHasher.hash("invalid-key")
        every { apiKeyRepository.findByKeyHash(key) } returns null

        mvc.get("/client/ping") {
            header("X-Api-Key", "invalid-key")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}

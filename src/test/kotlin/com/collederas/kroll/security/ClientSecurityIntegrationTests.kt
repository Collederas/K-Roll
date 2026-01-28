package com.collederas.kroll.security

import com.collederas.kroll.core.configentry.audit.ConfigEntryHistoryListener
import com.collederas.kroll.security.apikey.ApiKeyEntity
import com.collederas.kroll.security.apikey.ApiKeyHasher
import com.collederas.kroll.security.apikey.ApiKeyRepository
import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import com.collederas.kroll.user.AppUserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@Import(ConfigEntryHistoryListener::class)
@ActiveProfiles("test")
class ClientSecurityIntegrationTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var apiKeyRepository: ApiKeyRepository

    @Autowired
    lateinit var userRepository: AppUserRepository

    @Autowired
    lateinit var envFactory: PersistedEnvironmentFactory

    private val testedEndpoint = "/client/config/fetch"

    @Test
    fun `public route - client api is not accessible without token`() {
        envFactory.create()

        mvc
            .post(testedEndpoint)
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `api key route - valid api key returns 200`() {
        val env = envFactory.create()

        val rawKey = "api_key_12345"
        val hashedKey = ApiKeyHasher.hash(rawKey)

        apiKeyRepository.save(
            ApiKeyEntity(
                environment = env,
                keyHash = hashedKey,
                mask = "rk_l...2345",
                expiresAt = Instant.now().plusSeconds(3600),
            ),
        )

        mvc
            .post(testedEndpoint) {
                header("X-Api-Key", rawKey)
            }.andExpect { status { isOk() } }
    }

    @Test
    fun `api key route - invalid api key returns 401`() {
        envFactory.create()

        mvc
            .post(testedEndpoint) {
                header("X-Api-Key", "invalid-key")
            }.andExpect { status { isUnauthorized() } }
    }
}

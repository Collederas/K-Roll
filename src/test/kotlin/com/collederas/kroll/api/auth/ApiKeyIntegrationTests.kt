package com.collederas.kroll.api.auth

import com.collederas.kroll.security.apikey.ApiKeyRepository
import com.collederas.kroll.security.apikey.ApiKeyService
import com.collederas.kroll.security.apikey.dto.CreateApiKeyRequest
import com.collederas.kroll.support.MutableTestClock
import com.collederas.kroll.support.TestClockConfig
import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import com.jayway.jsonpath.JsonPath
import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Duration

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig::class)
@Transactional
@ActiveProfiles("test")
class ApiKeyIntegrationTests {
    @Autowired
    private lateinit var apiKeyRepository: ApiKeyRepository

    @Autowired
    lateinit var envFactory: PersistedEnvironmentFactory

    @Autowired
    lateinit var apiKeyService: ApiKeyService

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var clock: MutableTestClock

    private val protectedEndpoint = "/client/config/fetch"

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `create api key persists key and returns 201 with key metadata`() {
        val env = envFactory.create()
        val expiresAt = clock.instant().plus(Duration.ofDays(1))

        val createResult =
            mvc
                .post("/api/environments/${env.id}/api-keys") {
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        """
                        {
                          "expiresAt": "$expiresAt"
                        }
                        """.trimIndent()
                }.andExpect {
                    status { isCreated() }
                }.andReturn()

        val id =
            JsonPath.read<String>(
                createResult.response.contentAsString,
                "$.id",
            )

        val key =
            JsonPath.read<String>(
                createResult.response.contentAsString,
                "$.key",
            )

        Assertions.assertThat(id).isNotBlank()
        Assertions.assertThat(key).startsWith("rk_")

        val keys = apiKeyRepository.findAll()

        Assertions.assertThat(keys).hasSize(1)

        val createdKey = keys.first()
        Assertions.assertThat(createdKey.environment.id).isEqualTo(env.id)
        Assertions.assertThat(createdKey.expiresAt).isEqualTo(expiresAt)
    }

    @Test
    fun `deleted api key is immediately invalidated`() {
        val env = envFactory.create()
        val dto = CreateApiKeyRequest(clock.instant().plus(Duration.ofDays(1)))
        val created = apiKeyService.create(env.id, dto)

        mvc
            .post(protectedEndpoint) {
                header("X-Api-Key", created.key)
            }.andExpect {
                status { isOk() }
            }

        apiKeyService.delete(created.id)

        mvc
            .post(protectedEndpoint) {
                header("X-Api-Key", created.key)
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `expired api key does not authenticate`() {
        val env = envFactory.create()
        val expiresAt = clock.instant().plusSeconds(10)

        val createResult =
            mvc
                .post("/api/environments/${env.id}/api-keys") {
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        """
                        {
                          "expiresAt": "$expiresAt"
                        }
                        """.trimIndent()
                }.andReturn()

        val rawKey =
            JsonPath.read<String>(
                createResult.response.contentAsString,
                "$.key",
            )

        apiKeyRepository.flush()
        clock.advanceBy(Duration.ofSeconds(11))

        mvc
            .post(protectedEndpoint) {
                header("X-Api-Key", rawKey)
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `unknown api key does not authenticate`() {
        mvc
            .post(protectedEndpoint) {
                header("X-Api-Key", "rk_definitely_not_existing")
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `create returns raw key but list does not`() {
        val env = envFactory.create()
        val expiresAt = clock.instant().plus(Duration.ofDays(1))

        val createResult =
            mvc
                .post("/api/environments/${env.id}/api-keys") {
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        """
                        {
                          "expiresAt": "$expiresAt"
                        }
                        """.trimIndent()
                }.andReturn()

        val rawKey =
            JsonPath.read<String>(
                createResult.response.contentAsString,
                "$.key",
            )

        mvc
            .get("/api/environments/${env.id}/api-keys")
            .andExpect {
                MockMvcResultMatchers.jsonPath("$[*].truncated").exists()
                MockMvcResultMatchers.jsonPath("$[*].key").doesNotExist()
                MockMvcResultMatchers.jsonPath("$[*].keyHash").doesNotExist()
            }

        Assertions.assertThat(rawKey).startsWith("rk_")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `delete api key is idempotent`() {
        val env = envFactory.create()
        val expiresAt = clock.instant().plusSeconds(10)

        val createResult =
            mvc
                .post("/api/environments/${env.id}/api-keys") {
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        """
                        {
                          "expiresAt": "$expiresAt"
                        }
                        """.trimIndent()
                }.andReturn()

        val keyId =
            JsonPath.read<String>(
                createResult.response.contentAsString,
                "$.id",
            )

        mvc
            .delete("/api/environments/${env.id}/api-keys/$keyId")
            .andExpect { status { isNoContent() } }

        mvc
            .delete("/api/environments/${env.id}/api-keys/$keyId")
            .andExpect { status { isNoContent() } }
    }
}

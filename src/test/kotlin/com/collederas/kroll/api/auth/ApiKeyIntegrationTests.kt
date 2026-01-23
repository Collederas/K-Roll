package com.collederas.kroll.api.auth

import com.collederas.kroll.security.apikey.ApiKeyConfigProperties
import com.collederas.kroll.security.apikey.ApiKeyRepository
import com.collederas.kroll.security.apikey.ApiKeyService
import com.collederas.kroll.security.apikey.dto.ApiKeyAuthResult
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.Duration
import java.util.*

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
    private lateinit var apiKeyProperties: ApiKeyConfigProperties

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
                .post("/environments/${env.id}/api-keys") {
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
    @WithMockUser(roles = ["ADMIN"])
    fun `list returns correct metadata for active expiring api key`() {
        val env = envFactory.create()
        val now = clock.instant()
        val expiresAt = now.plus(Duration.ofHours(1))

        val created =
            apiKeyService.create(
                env.id,
                CreateApiKeyRequest(expiresAt = expiresAt),
            )

        val listResult =
            mvc
                .get("/environments/${env.id}/api-keys")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.length()").value(1)

                    jsonPath("$[0].id").value(created.id.toString())
                    jsonPath("$[0].environmentId").value(env.id.toString())

                    jsonPath("$[0].createdAt").exists()
                    jsonPath("$[0].expiresAt").value(expiresAt.toString())

                    jsonPath("$[0].neverExpires").value(false)
                    jsonPath("$[0].isActive").value(true)
                }.andReturn()

        val respKey =
            JsonPath.read<String>(
                listResult.response.contentAsString,
                "$[0].truncated",
            )
        Assertions.assertThat(respKey).startsWith("rk_")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `list marks expired api key as inactive`() {
        val env = envFactory.create()
        val expiresAt = clock.instant().plusSeconds(5)

        apiKeyService.create(
            env.id,
            CreateApiKeyRequest(expiresAt = expiresAt),
        )

        clock.advanceBy(Duration.ofSeconds(6))

        mvc
            .get("/environments/${env.id}/api-keys")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].expiresAt").value(expiresAt.toString())
                jsonPath("$[0].isActive").value(false)
            }
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
                .post("/environments/${env.id}/api-keys") {
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
                .post("/environments/${env.id}/api-keys") {
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
            .get("/environments/${env.id}/api-keys")
            .andExpect {
                jsonPath("$[*].truncated").exists()
                jsonPath("$[*].key").doesNotExist()
                jsonPath("$[*].keyHash").doesNotExist()
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
                .post("/environments/${env.id}/api-keys") {
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
            .delete("/environments/${env.id}/api-keys/$keyId")
            .andExpect { status { isNoContent() } }

        mvc
            .delete("/environments/${env.id}/api-keys/$keyId")
            .andExpect { status { isNoContent() } }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `can create api key with no expiration`() {
        val env = envFactory.create()

        val result =
            mvc
                .post("/environments/${env.id}/api-keys") {
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        """
                        {
                          "neverExpires": true
                        }
                        """.trimIndent()
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.id") { exists() }
                    jsonPath("$.key") { isNotEmpty() }
                    jsonPath("$.expiresAt") { doesNotExist() }
                    jsonPath("$.neverExpires") { value(true) }
                }.andReturn()

        val rawKey =
            JsonPath.read<String>(
                result.response.contentAsString,
                "$.key",
            )

        val authResult = apiKeyService.validate(rawKey)
        Assertions.assertThat { authResult is ApiKeyAuthResult.Valid }
    }

    @Test
    fun `never expiring api key works indefinitely`() {
        val env = envFactory.create()
        val dto = CreateApiKeyRequest(neverExpires = true)
        val created = apiKeyService.create(env.id, dto)

        mvc
            .post(protectedEndpoint) {
                header("X-Api-Key", created.key)
            }.andExpect {
                status { isOk() }
            }

        val beyondMaxLifetime = apiKeyProperties.maxLifetime + Duration.ofDays(1)

        clock.advanceBy(beyondMaxLifetime)

        mvc
            .post(protectedEndpoint) {
                header("X-Api-Key", created.key)
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `default lifetime is applied when payload is empty`() {
        val env = envFactory.create()

        val createResult =
            mvc
                .post("/environments/${env.id}/api-keys") {
                    with(user("admin").roles("ADMIN"))
                    contentType = MediaType.APPLICATION_JSON
                    content = "{}"
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.neverExpires").value(false)
                }.andReturn()

        val rawKey = JsonPath.read<String>(createResult.response.contentAsString, "$.key")

        val keyId = JsonPath.read<String>(createResult.response.contentAsString, "$.id")
        val createdKey = apiKeyRepository.findById(UUID.fromString(keyId)).get()

        val expectedExpiry = clock.instant().plus(apiKeyProperties.defaultLifetime)
        Assertions
            .assertThat(createdKey.expiresAt)
            .isCloseTo(expectedExpiry, Assertions.within(Duration.ofMinutes(1)))

        // The key must work
        mvc
            .post(protectedEndpoint) { header("X-Api-Key", rawKey) }
            .andExpect { status { isOk() } }
    }
}

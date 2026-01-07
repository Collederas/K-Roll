package com.collederas.kroll.security.apikey

import com.collederas.kroll.support.MutableTestClock
import com.collederas.kroll.support.TestClockConfig
import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.Duration
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig::class)
@ActiveProfiles("test")
class ApiKeyIntegrationTests {
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
    fun `deleted api key is immediately invalidated`() {
        val env = envFactory.create()
        val created = apiKeyService.create(env.id, Instant.now().plus(Duration.ofDays(1)))

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
    fun `expired api key does not authenticate`() {
        val env = envFactory.create()

        val key =
            apiKeyService.create(
                env.id,
                clock.instant().plusSeconds(10),
            )

        clock.advanceBy(Duration.ofSeconds(11))

        mvc
            .post(protectedEndpoint) {
                header("X-Api-Key", key.key)
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
                .post("/admin/environments/${env.id}/api-keys") {
                    contentType = MediaType.APPLICATION_JSON
                    content = "\"$expiresAt\""
                }.andReturn()

        val rawKey =
            JsonPath.read<String>(
                createResult.response.contentAsString,
                "$.key",
            )

        mvc
            .get("/admin/environments/${env.id}/api-keys")
            .andExpect {
                jsonPath("$[*].truncated").exists()
                jsonPath("$[*].key").doesNotExist()
                jsonPath("$[*].keyHash").doesNotExist()
            }

        assertThat(rawKey).startsWith("rk_")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `delete api key is idempotent`() {
        val env = envFactory.create()
        val key = apiKeyService.create(env.id, clock.instant().plusSeconds(10))

        mvc
            .delete("/admin/environments/${env.id}/api-keys/${key.id}")
            .andExpect { status { isNoContent() } }

        mvc
            .delete("/admin/environments/${env.id}/api-keys/${key.id}")
            .andExpect { status { isNoContent() } }
    }
}

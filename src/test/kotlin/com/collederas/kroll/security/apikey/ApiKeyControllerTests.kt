package com.collederas.kroll.security.apikey

import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiKeyControllerTests() {
    @Autowired
    lateinit var apiKeyService: ApiKeyService

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var envFactory: PersistedEnvironmentFactory

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `create api key returns raw key and metadata`() {
        val env = envFactory.create()
        val expiresAt = Instant.now().plus(Duration.ofDays(1))

        mvc.post("/admin/environments/${env.id}/api-keys") {
            contentType = MediaType.APPLICATION_JSON
            content = "\"$expiresAt\""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id").exists()
            jsonPath("$.key").exists()
            jsonPath("$.expiresAt").value(expiresAt.toString())
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `create returns raw key but list does not`() {
        val env = envFactory.create()
        val expiresAt = Instant.now().plus(Duration.ofDays(1))

        val createResult =
            mvc.post("/admin/environments/${env.id}/api-keys") {
                contentType = MediaType.APPLICATION_JSON
                content = "\"$expiresAt\""
            }.andReturn()

        val rawKey =
            JsonPath.read<String>(
                createResult.response.contentAsString,
                "$.key",
            )

        mvc.get("/admin/environments/${env.id}/api-keys")
            .andExpect {
                jsonPath("$[*].truncated").exists()
                jsonPath("$[*].key").doesNotExist()
                jsonPath("$[*].keyHash").doesNotExist()
            }

        assertTrue(rawKey.startsWith("rk_"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `list api keys never exposes secrets`() {
        val env = envFactory.create()
        apiKeyService.create(env.id, Instant.now().plus(Duration.ofDays(1)))

        mvc.get("/admin/environments/${env.id}/api-keys")
            .andExpect {
                jsonPath("$[0].id").exists()
                jsonPath("$[0].truncated").exists()
                jsonPath("$[0].key").doesNotExist()
                jsonPath("$[0].keyHash").doesNotExist()
            }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `delete api key is idempotent`() {
        val env = envFactory.create()
        val created =
            apiKeyService.create(
                env.id,
                Instant.now().plus(Duration.ofDays(1)),
            )
        mvc.delete("/admin/apikey/${created.id}")
            .andExpect { status { isNoContent() } }

        mvc.delete("/admin/apikey/${created.id}")
            .andExpect { status { isNoContent() } }
    }
}

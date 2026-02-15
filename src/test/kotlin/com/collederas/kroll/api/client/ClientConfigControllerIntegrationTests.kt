package com.collederas.kroll.api.client

import com.collederas.kroll.core.config.ClientConfigFetchResponseDto
import com.collederas.kroll.core.config.ClientConfigMetaDto
import com.collederas.kroll.core.config.ClientConfigQueryService
import com.collederas.kroll.core.config.audit.ConfigEntryHistoryListener
import com.collederas.kroll.exceptions.PublishedConfigNotFoundException
import com.collederas.kroll.security.apikey.ApiKeyEntity
import com.collederas.kroll.security.apikey.ApiKeyHasher
import com.collederas.kroll.security.apikey.ApiKeyRepository
import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.transaction.Transactional
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
@Transactional
@Import(ConfigEntryHistoryListener::class)
@ActiveProfiles("test")
class ClientConfigControllerIntegrationTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var envFactory: PersistedEnvironmentFactory

    @Autowired
    lateinit var apiKeyRepository: ApiKeyRepository

    @MockkBean
    lateinit var clientConfigQueryService: ClientConfigQueryService

    @Test
    fun `fetch returns semantic values and snapshot metadata`() {
        val env = envFactory.create()
        val rawKey = "rk_client_shape_123"

        apiKeyRepository.save(
            ApiKeyEntity(
                environment = env,
                keyHash = ApiKeyHasher.hash(rawKey),
                mask = "rk_...0123",
                expiresAt = Instant.now().plusSeconds(3600),
            ),
        )

        val publishedAt = Instant.parse("2026-02-15T09:12:34Z")
        every { clientConfigQueryService.fetchPublishedConfig(env.id) } returns
            ClientConfigFetchResponseDto(
                meta =
                    ClientConfigMetaDto(
                        schemaVersion = 1,
                        activeSnapshotId = env.id,
                        activeSnapshotHash = "sha256:abc123",
                        publishedAt = publishedAt,
                        label = "v12",
                    ),
                values =
                    mapOf(
                        "coins" to 120.0,
                        "characters" to mapOf("zombie" to mapOf("health" to 120.0)),
                    ),
            )

        mvc
            .post("/client/config/fetch") {
                header("X-Api-Key", rawKey)
            }.andExpect {
                status { isOk() }
                jsonPath("$.meta.schema_version") { value(1) }
                jsonPath("$.meta.active_snapshot_id") { value(env.id.toString()) }
                jsonPath("$.meta.active_snapshot_hash") { value("sha256:abc123") }
                jsonPath("$.meta.published_at") { value(publishedAt.toString()) }
                jsonPath("$.meta.label") { value("v12") }

                jsonPath("$.values.coins") { value(120.0) }
                jsonPath("$.values.characters.zombie.health") { value(120.0) }
                jsonPath("$.values.coins.type") { doesNotExist() }
                jsonPath("$.values.coins.value") { doesNotExist() }
            }
    }

    @Test
    fun `fetch returns not found problem detail when no published config exists`() {
        val env = envFactory.create()
        val rawKey = "rk_client_not_found_123"

        apiKeyRepository.save(
            ApiKeyEntity(
                environment = env,
                keyHash = ApiKeyHasher.hash(rawKey),
                mask = "rk_...0123",
                expiresAt = Instant.now().plusSeconds(3600),
            ),
        )

        every { clientConfigQueryService.fetchPublishedConfig(env.id) } throws
            PublishedConfigNotFoundException("No published config")

        mvc
            .post("/client/config/fetch") {
                header("X-Api-Key", rawKey)
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.error_code") { value("PUBLISHED_CONFIG_NOT_FOUND") }
                jsonPath("$.title") { exists() }
                jsonPath("$.detail") { exists() }
                jsonPath("$.instance") { exists() }
            }
    }
}

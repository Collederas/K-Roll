package com.collederas.kroll.core.config

import com.collederas.kroll.core.config.versioning.ActiveVersion
import com.collederas.kroll.core.config.versioning.ActiveVersionRepository
import com.collederas.kroll.core.config.versioning.ConfigVersion
import com.collederas.kroll.core.config.versioning.ConfigVersionRepository
import com.collederas.kroll.exceptions.PublishedConfigNotFoundException
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockKExtension::class)
class ClientConfigQueryServiceTests {
    @MockK
    lateinit var activeVersionRepository: ActiveVersionRepository

    @MockK
    lateinit var versionRepository: ConfigVersionRepository

    @MockK
    lateinit var resolver: ConfigResolver

    @InjectMockKs
    lateinit var service: ClientConfigQueryService

    @Test
    fun `fetchPublishedConfig returns metadata and semantic values`() {
        val envId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        val publishedAt = Instant.parse("2026-02-15T09:12:34Z")

        every { activeVersionRepository.findById(envId) } returns
            Optional.of(
                ActiveVersion(
                    environmentId = envId,
                    activeVersionId = versionId,
                    publishedAt = publishedAt,
                ),
            )

        every { versionRepository.findById(versionId) } returns
            Optional.of(
                ConfigVersion(
                    seq = 12,
                    id = versionId,
                    envId = envId,
                    label = "v12",
                    contractHash = "hash123",
                ),
            )

        every { resolver.resolvePublishedValues(envId) } returns
            linkedMapOf(
                "characters" to mapOf("zombie" to mapOf("health" to 120.0)),
                "coins" to 120.0,
            )

        val result = service.fetchPublishedConfig(envId)

        assertEquals(1, result.meta.schemaVersion)
        assertEquals(versionId, result.meta.activeSnapshotId)
        assertEquals("hash123", result.meta.activeSnapshotHash)
        assertEquals(publishedAt, result.meta.publishedAt)
        assertEquals("v12", result.meta.label)
        assertEquals(120.0, result.values["coins"])
    }

    @Test
    fun `fetchPublishedConfig throws not found when active row is missing`() {
        val envId = UUID.randomUUID()
        every { activeVersionRepository.findById(envId) } returns Optional.empty()

        assertThrows(PublishedConfigNotFoundException::class.java) {
            service.fetchPublishedConfig(envId)
        }
    }
}

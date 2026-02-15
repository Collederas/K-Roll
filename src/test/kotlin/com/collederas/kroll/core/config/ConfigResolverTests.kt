package com.collederas.kroll.core.config

import com.collederas.kroll.core.config.versioning.ActiveVersion
import com.collederas.kroll.core.config.versioning.ActiveVersionRepository
import com.collederas.kroll.core.config.versioning.snapshot.ConfigSnapshotEntity
import com.collederas.kroll.core.config.versioning.snapshot.ConfigSnapshotRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class ConfigResolverTests {
    private val activeVersionRepository = mockk<ActiveVersionRepository>()
    private val snapshotRepository = mockk<ConfigSnapshotRepository>()
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val clock = Clock.fixed(Instant.parse("2026-02-10T00:00:00Z"), ZoneOffset.UTC)

    private val resolver =
        ConfigResolver(
            activeVersionRepository = activeVersionRepository,
            snapshotRepository = snapshotRepository,
            objectMapper = objectMapper,
            clock = clock,
        )

    @Test
    fun `resolve published preserves integer number values`() {
        val envId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        val active = ActiveVersion(environmentId = envId, activeVersionId = versionId)
        val snapshotJson =
            """
            {
              "values": {
                "coins": { "type": "NUMBER", "value": 120 }
              }
            }
            """.trimIndent()

        every { activeVersionRepository.findById(envId) } returns Optional.of(active)
        every { snapshotRepository.findByVersionId(versionId) } returns
            ConfigSnapshotEntity(versionId = versionId, snapshotJson = snapshotJson)

        val resolved = resolver.resolveForEnvironment(envId, ResolveMode.PUBLISHED)

        assertEquals(BigDecimal("120"), resolved.values["coins"]?.value)
    }

    @Test
    fun `resolve draft preserves decimal number values from numeric strings`() {
        val envId = UUID.randomUUID()
        val draftJson =
            objectMapper.readTree(
                """
                {
                  "values": {
                    "multiplier": { "type": "NUMBER", "value": "1.2500" }
                  }
                }
                """.trimIndent(),
            )
        val active = ActiveVersion(environmentId = envId, draftJson = draftJson)

        every { activeVersionRepository.findById(envId) } returns Optional.of(active)

        val resolved = resolver.resolveForEnvironment(envId, ResolveMode.DRAFT)

        assertEquals(BigDecimal("1.2500"), resolved.values["multiplier"]?.value)
    }

    @Test
    fun `resolve published values returns semantic map sorted by key and filtered by activation`() {
        val envId = UUID.randomUUID()
        val versionId = UUID.randomUUID()
        val active = ActiveVersion(environmentId = envId, activeVersionId = versionId)
        val snapshotJson =
            """
            {
              "values": {
                "zeta": { "type": "STRING", "value": "late" },
                "alpha": { "type": "BOOLEAN", "value": true },
                "beta": { "type": "JSON", "value": { "x": 1 } },
                "future": {
                  "type": "NUMBER",
                  "value": 10,
                  "activeFrom": "2026-03-01T00:00:00Z"
                }
              }
            }
            """.trimIndent()

        every { activeVersionRepository.findById(envId) } returns Optional.of(active)
        every { snapshotRepository.findByVersionId(versionId) } returns
            ConfigSnapshotEntity(versionId = versionId, snapshotJson = snapshotJson)

        val values = resolver.resolvePublishedValues(envId)

        assertEquals(listOf("alpha", "beta", "zeta"), values.keys.toList())
        assertEquals(true, values["alpha"])
        assertEquals(mapOf("x" to 1), values["beta"])
        assertEquals("late", values["zeta"])
    }
}

package com.collederas.kroll.core.config

import com.collederas.kroll.core.config.diff.ConfigDiffCalculator
import com.collederas.kroll.core.config.diff.DiffEntry
import com.collederas.kroll.core.config.diff.DiffResult
import com.collederas.kroll.core.config.diff.SemanticDiff
import com.collederas.kroll.core.config.entry.ConfigType
import com.collederas.kroll.core.config.versioning.ActiveVersion
import com.collederas.kroll.core.config.versioning.ActiveVersionRepository
import com.collederas.kroll.core.config.versioning.ConfigEntry
import com.collederas.kroll.core.config.versioning.ConfigVersion
import com.collederas.kroll.core.config.versioning.ConfigVersionRepository
import com.collederas.kroll.core.config.versioning.ConfigVersionService
import com.collederas.kroll.core.config.versioning.snapshot.ConfigSnapshotEntity
import com.collederas.kroll.core.config.versioning.snapshot.ConfigSnapshotRepository
import com.collederas.kroll.exceptions.ExistingUnpublishedDraft
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.*


@ExtendWith(MockKExtension::class)
class ConfigVersionServiceTests {
    @MockK
    lateinit var configResolver: ConfigResolver
    @MockK
    lateinit var versionRepository: ConfigVersionRepository
    @MockK
    lateinit var activeVersionRepository: ActiveVersionRepository
    @MockK
    lateinit var snapshotRepository: ConfigSnapshotRepository
    @MockK
    lateinit var configDiffCalculator: ConfigDiffCalculator

    @MockK
    lateinit var objectMapper: ObjectMapper

    @InjectMockKs
    lateinit var service: ConfigVersionService

    private val envId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val hash1 = "hash1"
    private val hash2 = "hash2"
    fun contractHash(): String = UUID.randomUUID().toString().replace("-", "")


    @Test
    fun `listAllVersions marks active version correctly`() {
        val v1 = ConfigVersion(seq = 1, envId = envId, contractHash = hash1)
        val v2 = ConfigVersion(seq = 2, envId = envId, contractHash = hash2)

        every {
            versionRepository.findAllByEnvironmentIdOrderByVersionSequenceDesc(envId)
        } returns listOf(v2, v1)

        every {
            activeVersionRepository.findByEnvironmentId(envId)
        } returns ActiveVersion(
            environmentId = envId,
            activeVersionId = v1.id,
            publishedAt = Instant.now(),
        )

        val result = service.listAllVersions(envId)

        assertTrue(result.first { it.id == v1.id }.isActive)
        assertFalse(result.first { it.id == v2.id }.isActive)
    }

    @Test
    fun `publishNewVersion creates next version and snapshot`() {
        val resolvedConfig = mockk<ResolvedConfig> {
            every { values } returns emptyMap()
        }

        every {
            configResolver.resolveForEnvironment(envId, ResolveMode.DRAFT)
        } returns resolvedConfig

        every {
            versionRepository.findLatestByEnvironmentId(envId)
        } returns ConfigVersion(
            seq = 1,
            envId = envId,
            contractHash = hash1,
        )

        every { objectMapper.writeValueAsBytes(any()) } returns ByteArray(0)
        every { objectMapper.writeValueAsString(any()) } returns "{}"
        every { versionRepository.save(any()) } answers { firstArg() }
        every { snapshotRepository.save(any()) } returns mockk()

        service.publishNewVersion(userId, envId, "notes")

        verify {
            versionRepository.save(match {
                it.versionSequence == 2L &&
                    it.versionLabel == "v2" &&
                    it.parentHash == hash1 &&
                    it.createdBy == userId
            })
            snapshotRepository.save(any<ConfigSnapshotEntity>())
        }
    }

    @Test
    fun `promoteVersion fails if draft is dirty and not forced`() {
        val version = ConfigVersion(seq = 1, envId = envId, contractHash = hash1)

        every { versionRepository.findById(version.id) } returns Optional.of(version)
        every { snapshotRepository.existsById(version.id) } returns true
        every {
            activeVersionRepository.findLocked(envId)
        } returns ActiveVersion(
            environmentId = envId,
            draftJson = mockk(),
            draftUpdatedAt = Instant.now(),
        )

        assertThrows(ExistingUnpublishedDraft::class.java) {
            service.promoteVersion(envId, version.id, userId, force = false)
        }
    }

    @Test
    fun `promoteVersion updates active version and clears draft`() {
        val version = ConfigVersion(seq = 1, envId = envId, contractHash = hash1)

        val active = ActiveVersion(
            environmentId = envId,
            draftJson = mockk(),
            draftUpdatedAt = Instant.now(),
        )

        every { versionRepository.findById(version.id) } returns Optional.of(version)
        every { snapshotRepository.existsById(version.id) } returns true
        every { activeVersionRepository.findLocked(envId) } returns active

        service.promoteVersion(envId, version.id, userId, force = true)

        assertEquals(version.id, active.activeVersionId)
        assertEquals(userId, active.publishedBy)
        assertNull(active.draftJson)
        assertNull(active.draftUpdatedAt)
        assertNull(active.draftUpdatedBy)
    }

    @Test
    fun `getActiveVersion returns null when no active version`() {
        every {
            activeVersionRepository.findByEnvironmentId(envId)
        } returns ActiveVersion(environmentId = envId)

        assertNull(service.getActiveVersion(envId))
    }

    @Test
    fun `getVersionDetails returns version and snapshot`() {
        val version = ConfigVersion(seq = 1, envId = envId, contractHash = hash1)
        val snapshot = ConfigSnapshotEntity(
            versionId = version.id,
            snapshotJson = "{}",
        )

        every { versionRepository.findById(version.id) } returns Optional.of(version)
        every { snapshotRepository.findById(version.id) } returns Optional.of(snapshot)

        val result = service.getVersionDetails(envId, version.id)

        assertEquals(version.id, result.id)
        assertEquals("{}", result.snapshotJson)
    }

    @Test
    fun `diffVersions classifies added removed type and value changes`() {
        val v1 = ConfigVersion(seq = 1, envId = envId, contractHash = hash1, label = "v1")
        val v2 = ConfigVersion(seq = 2, envId = envId, contractHash = hash2, label = "v2")

        every { versionRepository.findById(v1.id) } returns Optional.of(v1)
        every { versionRepository.findById(v2.id) } returns Optional.of(v2)
        every { snapshotRepository.findById(any()) } returns Optional.of(
            ConfigSnapshotEntity(versionId = UUID.randomUUID(), snapshotJson = "{}")
        )

        every {
            configDiffCalculator.diffSnapshots(any(), any())
        } returns listOf(
            DiffResult.Added(
                key = "a",
                entry = ConfigEntry(ConfigType.STRING, "x"),
            ),
            DiffResult.Removed(
                key = "b",
                entry = ConfigEntry(ConfigType.NUMBER, 1),
            ),
            DiffResult.Changed(
                key = "c",
                old = ConfigEntry(ConfigType.STRING, "x"),
                new = ConfigEntry(ConfigType.STRING, "y"),
                semantic = SemanticDiff.ValueChanged,
            ),
            DiffResult.Changed(
                key = "d",
                old = ConfigEntry(ConfigType.STRING, "x"),
                new = ConfigEntry(ConfigType.NUMBER, 1),
                semantic = SemanticDiff.TypeChanged,
            ),
        )

        val diff = service.diffVersions(envId, v1.id, v2.id)

        assertEquals(setOf("a"), diff.added)
        assertEquals(setOf("b"), diff.removed)
        assertEquals(1, diff.valueChanged.size)
        assertEquals(1, diff.typeChanged.size)
    }

}

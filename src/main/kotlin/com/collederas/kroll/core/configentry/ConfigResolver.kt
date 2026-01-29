package com.collederas.kroll.core.configentry

import com.collederas.kroll.core.configentry.entries.ConfigEntryEntity
import com.collederas.kroll.core.configentry.entries.ConfigEntryRepository
import com.collederas.kroll.core.configentry.entries.ConfigType
import com.collederas.kroll.core.configentry.versioning.ActiveVersionEntity
import com.collederas.kroll.core.configentry.versioning.ActiveVersionRepository
import com.collederas.kroll.core.configentry.versioning.ConfigVersionRepository
import com.collederas.kroll.core.configentry.versioning.snapshot.ConfigSnapshotRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class ResolvedConfig(
    val values: Map<String, ResolvedValue>
)

data class ResolvedValue(
    val type: ConfigType,
    val value: Any
)

enum class ResolveMode {
    PUBLISHED,
    DRAFT,
}

@Component
class ConfigResolver(
    private val activeVersionRepository: ActiveVersionRepository,
    private val snapshotRepository: ConfigSnapshotRepository,
    private val entryRepository: ConfigEntryRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun resolveForEnvironment(
        envId: UUID,
        mode: ResolveMode,
    ): ResolvedConfig =
        when (mode) {
            ResolveMode.PUBLISHED -> resolvePublished(envId)
            ResolveMode.DRAFT -> resolveDraft(envId)
        }

    fun resolvePublished(envId: UUID): ResolvedConfig {
        val active =
            activeVersionRepository.findById(envId)
                .orElseThrow { error("No active version for env $envId") }

        val versionId =
            active.activeVersionId
                ?: error("No published version for env $envId")

        val snapshot =
            snapshotRepository.findByVersionId(versionId)
                ?: error("No snapshot for version $versionId")

        return objectMapper.readValue(
            snapshot.snapshotJson,
            ResolvedConfig::class.java
        )
    }

    fun resolveDraft(envId: UUID): ResolvedConfig {
        val now = clock.instant()

        val resolved =
            entryRepository
                .findAllByEnvironmentId(envId)
                .asSequence()
                .filter { it.activeFrom == null || !it.activeFrom!!.isAfter(now) }
                .filter { it.activeUntil == null || it.activeUntil!!.isAfter(now) }
                .groupBy { it.configKey }
                .mapValues { (_, entries) ->
                    val e = entries.maxBy { it.createdAt }
                    ResolvedValue(e.configType, parseValue(e))
                }
                .toSortedMap()

        return ResolvedConfig(resolved)
    }

    private fun parseValue(entry: ConfigEntryEntity): Any =
        when (entry.configType) {
            ConfigType.BOOLEAN -> entry.configValue.toBooleanStrict()
            ConfigType.NUMBER -> entry.configValue.toBigDecimal()
            ConfigType.STRING -> entry.configValue
            ConfigType.JSON ->
                objectMapper.readValue(entry.configValue, Any::class.java)
        }
}


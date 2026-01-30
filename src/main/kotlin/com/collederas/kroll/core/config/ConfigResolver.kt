package com.collederas.kroll.core.config

import com.collederas.kroll.core.config.entry.ConfigType
import com.collederas.kroll.core.config.versioning.ActiveVersionRepository
import com.collederas.kroll.core.config.versioning.snapshot.ConfigSnapshotRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.*

data class ResolvedConfig(
    val values: Map<String, ResolvedValue>,
)

data class ResolvedValue(
    val type: ConfigType,
    val value: Any,
)

enum class ResolveMode {
    PUBLISHED,
    DRAFT,
}

@Component
class ConfigResolver(
    private val activeVersionRepository: ActiveVersionRepository,
    private val snapshotRepository: ConfigSnapshotRepository,
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

    private fun resolvePublished(envId: UUID): ResolvedConfig {
        val active =
            activeVersionRepository
                .findById(envId)
                .orElseThrow { error("No active version row for env $envId") }

        val versionId =
            active.activeVersionId
                ?: error("No published version for env $envId")

        val snapshot =
            snapshotRepository.findByVersionId(versionId)
                ?: error("No snapshot for version $versionId")

        val json =
            objectMapper.readTree(snapshot.snapshotJson)

        return resolveFromJson(json)
    }

    private fun resolveDraft(envId: UUID): ResolvedConfig {
        val active =
            activeVersionRepository
                .findById(envId)
                .orElseThrow { error("No active version row for env $envId") }

        val draft =
            active.draftJson
                ?: error("No draft exists for env $envId")

        return resolveFromJson(draft)
    }

    private fun resolveFromJson(source: JsonNode): ResolvedConfig {
        val now = clock.instant()
        val result = mutableMapOf<String, ResolvedValue>()

        val valuesNode =
            source["values"]
                ?: return ResolvedConfig(emptyMap())

        require(valuesNode.isObject) {
            "'values' must be a JSON object"
        }

        valuesNode.properties().forEach { (key, entry) ->
            val type = ConfigType.valueOf(entry["type"].asText())
            val valueNode = entry["value"]

            val activeFrom =
                entry["activeFrom"]
                    ?.takeIf { !it.isNull }
                    ?.let { Instant.parse(it.asText()) }

            val activeUntil =
                entry["activeUntil"]
                    ?.takeIf { !it.isNull }
                    ?.let { Instant.parse(it.asText()) }

            if (activeFrom != null && activeFrom.isAfter(now)) return@forEach
            if (activeUntil != null && !activeUntil.isAfter(now)) return@forEach

            result[key] =
                ResolvedValue(
                    type = type,
                    value = parseValue(type, valueNode),
                )
        }

        return ResolvedConfig(result.toSortedMap())
    }

    private fun parseValue(
        type: ConfigType,
        node: JsonNode,
    ): Any =
        when (type) {
            ConfigType.BOOLEAN ->
                node.booleanValue()

            ConfigType.NUMBER ->
                node.floatValue()

            ConfigType.STRING ->
                node.textValue()

            ConfigType.JSON -> objectMapper.treeToValue(node, Any::class.java)
        }
}

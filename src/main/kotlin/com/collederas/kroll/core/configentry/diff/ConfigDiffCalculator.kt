package com.collederas.kroll.core.configentry.diff

import com.collederas.kroll.core.configentry.entries.ConfigType
import com.collederas.kroll.core.configentry.versioning.snapshot.ConfigSnapshotEntity
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component


@Component
class ConfigDiffCalculator(
    @Qualifier("strictJsonMapper")
    private val objectMapper: ObjectMapper,
) {
    private data class SnapshotPayload(
        val values: Map<String, SnapshotValue>
    )

    private data class SnapshotValue(
        val type: ConfigType,
        val value: Any
    )

    fun diffSnapshots(
        from: ConfigSnapshotEntity,
        to: ConfigSnapshotEntity,
    ): List<DiffResult> {
        val fromPayload: SnapshotPayload =
            objectMapper.readValue(from.snapshotJson)

        val toPayload: SnapshotPayload =
            objectMapper.readValue(to.snapshotJson)

        return diffEntries(fromPayload.values, toPayload.values)
    }

    private fun diffEntries(
        old: Map<String, SnapshotValue>,
        new: Map<String, SnapshotValue>,
    ): List<DiffResult> {
        val allKeys = old.keys union new.keys

        return allKeys.mapNotNull { key ->
            val o = old[key]
            val n = new[key]

            when {
                o == null && n != null ->
                    DiffResult.Added(
                        key,
                        DiffEntry(key, n.type, n.value),
                    )

                o != null && n == null ->
                    DiffResult.Removed(
                        key,
                        DiffEntry(key, o.type, o.value),
                    )

                o != null && n != null -> {
                    if (o.type != n.type) {
                        DiffResult.Changed(
                            key,
                            DiffEntry(key, o.type, o.value),
                            DiffEntry(key, n.type, n.value),
                            SemanticDiff.TypeChanged,
                        )
                    } else {
                        val semantic = semanticCompare(o.value, n.value, o.type)
                        if (semantic is SemanticDiff.Same) null
                        else DiffResult.Changed(
                            key,
                            DiffEntry(key, o.type, o.value),
                            DiffEntry(key, n.type, n.value),
                            semantic,
                        )
                    }
                }

                else -> null
            }
        }.sortedBy { it.key }
    }



    fun semanticCompare(
        oldValue: Any,
        newValue: Any,
        type: ConfigType,
    ): SemanticDiff =
        try {
            when (type) {
                ConfigType.BOOLEAN ->
                    if (oldValue as Boolean == newValue as Boolean)
                        SemanticDiff.Same else SemanticDiff.ValueChanged

                ConfigType.NUMBER ->
                    if ((oldValue as Number).toDouble() ==
                        (newValue as Number).toDouble()
                    )
                        SemanticDiff.Same else SemanticDiff.ValueChanged

                ConfigType.STRING ->
                    if (oldValue == newValue)
                        SemanticDiff.Same else SemanticDiff.ValueChanged

                ConfigType.JSON -> {
                    val n1 = objectMapper.valueToTree<JsonNode>(oldValue)
                    val n2 = objectMapper.valueToTree<JsonNode>(newValue)
                    if (n1 == n2)
                        SemanticDiff.Same else SemanticDiff.ValueChanged
                }
            }
        } catch (e: Exception) {
            SemanticDiff.Invalid(e)
        }
}

package com.collederas.kroll.core.configentry.diff

import com.collederas.kroll.core.configentry.ConfigDiff
import com.collederas.kroll.core.configentry.entries.ConfigType
import com.collederas.kroll.core.configentry.audit.ConfigEntrySnapshot
import com.collederas.kroll.core.configentry.versioning.snapshot.ConfigSnapshotEntity
import com.collederas.kroll.core.configentry.versioning.snapshot.ConfigSnapshotRepository
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ConfigDiffCalculator(
    @Qualifier("strictJsonMapper") private val strictJsonMapper: ObjectMapper,
) {
    fun diffEntrySnapshots(
        old: List<ConfigEntrySnapshot>,
        new: List<ConfigEntrySnapshot>,
    ): List<EntryDiff> {
        val oldByKey = old.associateBy { it.key }
        val newByKey = new.associateBy { it.key }

        val allKeys = oldByKey.keys + newByKey.keys

        return allKeys.mapNotNull { key ->
            val o = oldByKey[key]
            val n = newByKey[key]

            when {
                o == null && n != null ->
                    EntryDiff.Added(key, n)

                o != null && n == null ->
                    EntryDiff.Removed(key, o)

                o != null && n != null -> {
                    val semantic = calcEntrySnapshotDiff(o, n)
                    if (semantic is SemanticDiff.Same) null
                    else EntryDiff.Changed(key, o, n, semantic)
                }

                else -> null
            }
        }.sortedBy { it.key }
    }

    private fun calcEntrySnapshotDiff(
        old: ConfigEntrySnapshot,
        new: ConfigEntrySnapshot,
    ): SemanticDiff {
        if (old.type != new.type) return SemanticDiff.Different

        return try {
            when (old.type) {
                ConfigType.BOOLEAN ->
                    if (old.value.toBooleanStrict() == new.value.toBooleanStrict())
                        SemanticDiff.Same else SemanticDiff.Different

                ConfigType.NUMBER ->
                    if (old.value.toBigDecimal() == new.value.toBigDecimal())
                        SemanticDiff.Same else SemanticDiff.Different

                ConfigType.STRING ->
                    if (old.value == new.value)
                        SemanticDiff.Same else SemanticDiff.Different

                ConfigType.JSON ->
                    jsonSemanticDiff(old.value, new.value)
            }
        } catch (e: Exception) {
            SemanticDiff.Invalid(e)
        }
    }

    fun jsonSemanticDiff(
        val1: String,
        val2: String,
    ): SemanticDiff =
        try {
            val node1 = strictJsonMapper.readTree(val1)
            val node2 = strictJsonMapper.readTree(val2)

            if (node1 == node2) {
                SemanticDiff.Same
            } else {
                SemanticDiff.Different
            }
        } catch (e: JsonProcessingException) {
            SemanticDiff.Invalid(e)
        }
}

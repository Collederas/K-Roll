package com.collederas.kroll.core.config.diff

import com.collederas.kroll.core.config.entry.ConfigType

sealed interface SemanticDiff {
    object Same : SemanticDiff // internal-only

    object ValueChanged : SemanticDiff

    object TypeChanged : SemanticDiff

    data class Invalid(
        val cause: Exception,
    ) : SemanticDiff
}

sealed class DiffResult {
    abstract val key: String

    data class Added(
        override val key: String,
        val entry: DiffEntry,
    ) : DiffResult()

    data class Removed(
        override val key: String,
        val entry: DiffEntry,
    ) : DiffResult()

    data class Changed(
        override val key: String,
        val old: DiffEntry,
        val new: DiffEntry,
        val semantic: SemanticDiff,
    ) : DiffResult()
}

data class DiffEntry(
    val key: String,
    val type: ConfigType,
    val value: Any, // primitives or structured JSON
)

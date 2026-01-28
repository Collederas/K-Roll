package com.collederas.kroll.core.configentry.diff

import com.collederas.kroll.core.configentry.audit.ConfigEntrySnapshot

sealed interface SemanticDiff {
    object Same : SemanticDiff

    object Different : SemanticDiff

    data class Invalid(
        val cause: Exception,
    ) : SemanticDiff
}


sealed interface EntryDiff {
    val key: String

    data class Added(
        override val key: String,
        val new: ConfigEntrySnapshot,
    ) : EntryDiff

    data class Removed(
        override val key: String,
        val old: ConfigEntrySnapshot,
    ) : EntryDiff

    data class Changed(
        override val key: String,
        val old: ConfigEntrySnapshot,
        val new: ConfigEntrySnapshot,
        val semantic: SemanticDiff,
    ) : EntryDiff
}

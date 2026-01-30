package com.collederas.kroll.core.config.draft

import com.collederas.kroll.core.config.validation.ConfigDraftValidator
import com.collederas.kroll.core.config.versioning.ActiveVersionEntity
import com.collederas.kroll.core.config.versioning.ActiveVersionRepository
import com.collederas.kroll.core.config.versioning.snapshot.ConfigSnapshotRepository
import com.collederas.kroll.exceptions.ConfigValidationException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonpatch.JsonPatch
import com.github.fge.jsonpatch.JsonPatchException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class ConfigDraftService(
    private val activeVersionRepository: ActiveVersionRepository,
    private val snapshotRepository: ConfigSnapshotRepository,
    private val draftValidator: ConfigDraftValidator,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun fetchOrInitializeDraft(envId: UUID): ConfigDraftResponseDto {
        val active =
            activeVersionRepository.findLocked(envId)

        if (active.draftJson == null) {
            active.draftJson = materializeDraft(active)
            active.draftUpdatedAt = Instant.now()
        }

        return ConfigDraftResponseDto.from(active)
    }

    private fun materializeDraft(active: ActiveVersionEntity): JsonNode {
        // No draft = empty state for designers
        val versionId =
            active.activeVersionId
                ?: return emptyDraftJson()

        val snapshot =
            snapshotRepository
                .findById(versionId)
                .orElseThrow {
                    IllegalStateException(
                        "active version $versionId has no snapshot",
                    )
                }

        return objectMapper.readTree(snapshot.snapshotJson)
    }

    @Transactional
    @PreAuthorize("@envAuth.isOwner(#envId, authentication.principal.userId)")
    fun applyPatch(
        userId: UUID,
        envId: UUID,
        patch: JsonPatch,
    ): ConfigDraftResponseDto {
        val active =
            activeVersionRepository
                .findLocked(envId)

        // Draft must exist before patching
        val currentDraft =
            active.draftJson ?: run {
                val initialized = materializeDraft(active)
                active.draftJson = initialized
                active.draftUpdatedAt = Instant.now()
                active.draftUpdatedBy = userId
                initialized
            }

        val patchedDraft =
            try {
                patch.apply(currentDraft)
            } catch (ex: JsonPatchException) {
                throw ConfigValidationException(
                    listOf("Invalid patch path ${ex.message}"),
                )
            }

        val violations = draftValidator.validate(patchedDraft)
        if (violations.isNotEmpty()) {
            throw ConfigValidationException(violations)
        }
        active.draftJson = patchedDraft
        active.draftUpdatedAt = Instant.now()
        active.draftUpdatedBy = userId

        return ConfigDraftResponseDto.from(active)
    }

    fun emptyDraftJson(): JsonNode =
        objectMapper.readTree(
            """
            {}
            """.trimIndent(),
        )
}

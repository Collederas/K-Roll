package com.collederas.kroll.api.admin

import com.collederas.kroll.core.config.draft.ConfigDraftResponseDto
import com.collederas.kroll.core.config.draft.ConfigDraftService
import com.collederas.kroll.security.identity.AuthUserDetails
import com.github.fge.jsonpatch.JsonPatch
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/environments/{envId}/draft")
@Tag(
    name = "Configuration Draft",
    description = "Edit and inspect the mutable configuration draft for an environment",
)
class ConfigDraftController(
    private val draftService: ConfigDraftService,
) {
    /**
     * Returns the editable draft for the environment.
     *
     * If no draft exists yet, the system materializes one from the
     * currently active published version inside the same transaction.
     */
    @GetMapping
    @Operation(
        summary = "Fetch configuration draft",
        description = "Returns the environment draft JSON, initializing it from the active version if needed",
    )
    fun fetchDraft(
        @PathVariable envId: UUID,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): ConfigDraftResponseDto = draftService.fetchOrInitializeDraft(envId)

    /**
     * Applies a patch to the draft.
     *
     * This endpoint is the ONLY way configuration can be edited.
     * No versions are created and no snapshots are written.
     */
    @PatchMapping
    @Operation(
        summary = "Patch configuration draft",
        description = "Applies a JSON Patch to the draft configuration",
    )
    fun patchDraft(
        @PathVariable envId: UUID,
        @Valid @RequestBody patch: JsonPatch,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): ConfigDraftResponseDto = draftService.applyPatch(authUser.getId(), envId, patch)
}

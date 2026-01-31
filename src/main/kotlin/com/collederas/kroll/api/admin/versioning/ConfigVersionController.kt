package com.collederas.kroll.api.admin.versioning

import com.collederas.kroll.core.config.ConfigDiffDto
import com.collederas.kroll.core.config.ConfigVersionDto
import com.collederas.kroll.core.config.VersionDetailsDto
import com.collederas.kroll.core.config.versioning.ConfigVersionService
import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.user.UserDirectory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/environments/{envId}/versions")
class ConfigVersionController(
    private val versionService: ConfigVersionService,
    private val userDirectory: UserDirectory,
) {
    @GetMapping
    fun listVersions(
        @PathVariable envId: UUID,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): List<ConfigVersionDto> {
        val versions = versionService.listAllVersions(envId)

        val creatorIds: Set<UUID> =
            versions.mapNotNull { it.createdBy }.toSet()

        val namesById: Map<UUID, String> =
            userDirectory.resolveDisplayNames(creatorIds)

        return versions.map { dto ->
            dto.copy(
                createdByName = dto.createdBy?.let(namesById::get),
            )
        }
    }

    @PostMapping
    fun publish(
        @PathVariable envId: UUID,
        @AuthenticationPrincipal user: AuthUserDetails,
        @RequestBody body: PublishVersionRequest,
    ) = versionService.publishNewVersion(
        userId = user.getId(),
        envId = envId,
        notes = body.notes,
    )

    @GetMapping("/{versionId}")
    fun getVersion(
        @PathVariable envId: UUID,
        @PathVariable versionId: UUID,
    ): VersionDetailsDto {
        val dto =
            versionService.getVersionDetails(
                envId,
                versionId,
            )
        val enriched =
            dto.createdBy?.let { userId ->
                dto.copy(
                    createdByName = userDirectory.resolveDisplayName(userId),
                )
            } ?: dto

        return enriched
    }

    @PostMapping("/{versionId}/promote")
    fun promote(
        @PathVariable envId: UUID,
        @PathVariable versionId: UUID,
        @RequestParam(name = "force", defaultValue = "false") force: Boolean,
        @AuthenticationPrincipal user: AuthUserDetails,
    ) = versionService.promoteVersion(
        envId = envId,
        versionId = versionId,
        promotedBy = user.getId(),
        force = force,
    )

    @PostMapping("/{versionId}/rollback")
    fun rollback(
        @PathVariable envId: UUID,
        @PathVariable versionId: UUID,
        @AuthenticationPrincipal user: AuthUserDetails,
    ) = versionService.rollbackToVersion(
        envId = envId,
        versionId = versionId,
        userId = user.getId(),
    )

    @GetMapping("/diff")
    fun diff(
        @PathVariable envId: UUID,
        @RequestParam from: UUID,
        @RequestParam to: UUID,
    ): ConfigDiffDto =
        versionService.diffVersions(
            envId,
            fromVersionId = from,
            toVersionId = to,
        )
}

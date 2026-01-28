package com.collederas.kroll.api.control.versioning

import com.collederas.kroll.core.configentry.ConfigDiff
import com.collederas.kroll.core.configentry.ConfigVersionDto
import com.collederas.kroll.core.configentry.VersionDetailsDto
import com.collederas.kroll.core.configentry.versioning.ConfigVersionService
import com.collederas.kroll.security.identity.AuthUserDetails
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/environments/{envId}/versions")
class ConfigVersionController(
    private val versionService: ConfigVersionService,
) {

    @GetMapping
    fun listVersions(
        @PathVariable envId: UUID,
        @AuthenticationPrincipal authUser: AuthUserDetails,
    ): List<ConfigVersionDto> =
        versionService.listAllVersions(envId)

    @PostMapping
    fun publish(
        @PathVariable envId: UUID,
        @AuthenticationPrincipal user: AuthUserDetails,
        @RequestBody body: PublishVersionRequest,
    ) =
        versionService.publishNewVersion(
            userId = user.getId(),
            envId = envId,
            notes = body.notes,
        )

    @GetMapping("/{version}")
    fun getVersion(
        @PathVariable envId: UUID,
        @PathVariable version: String,
    ): VersionDetailsDto =
        versionService.getVersionDetails(envId, version)

    @PostMapping("/{version}/promote")
    fun promote(
        @PathVariable envId: UUID,
        @PathVariable version: String,
        @AuthenticationPrincipal user: AuthUserDetails,
    ) =
        versionService.promoteVersion(
            envId = envId,
            version = version,
            promotedBy = user.getId(),
        )

    @PostMapping("/{version}/rollback")
    fun rollback(
        @PathVariable envId: UUID,
        @PathVariable version: String,
        @AuthenticationPrincipal user: AuthUserDetails,
    ) =
        versionService.rollbackToVersion(
            envId = envId,
            version = version,
            userId = user.getId(),
        )

    @GetMapping("/diff")
    fun diff(
        @PathVariable envId: UUID,
        @RequestParam from: String,
        @RequestParam to: String,
    ): ConfigDiff =
        versionService.diffVersions(envId, from, to)

}

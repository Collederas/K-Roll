package com.collederas.kroll.api.control.versioning

import com.collederas.kroll.core.configentry.ConfigVersionDto
import com.collederas.kroll.core.configentry.versioning.ConfigVersionService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

//@RestController
//@RequestMapping("/environments/{envId}/versions")
//class ConfigVersionController(
//    private val versionService: ConfigVersionService,
//) {
//
//    @GetMapping
//    fun listVersions(
//        @PathVariable envId: UUID,
//    ): List<ConfigVersionDto> =
//        versionService.listVersions(envId)
//}

package com.collederas.kroll.api.control.versioning

import com.collederas.kroll.core.configentry.ConfigSnapshotResponseDto
import com.collederas.kroll.core.configentry.versioning.snapshot.ConfigSnapshotService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

//
//@RestController
//@RequestMapping("/environments/{envId}/versions")
//class ConfigSnapshotController(
//    private val snapshotService: ConfigSnapshotService,
//) {
//
//    @GetMapping("/{versionId}")
//    fun getSnapshot(
//        @PathVariable envId: UUID,
//        @PathVariable versionId: String,
//    ): ConfigSnapshotResponseDto =
//        snapshotService.getSnapshot(envId, versionId)
//}


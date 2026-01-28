package com.collederas.kroll.api.control.versioning

data class PublishVersionRequest(
    val userId: String,
    val environmentId: String,
    val notes: String? = null,
)

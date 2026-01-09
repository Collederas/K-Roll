package com.collederas.kroll.security.apikey.identity

import java.util.UUID

data class GameClientPrincipal(
    val environmentId: UUID,
    val apiKeyId: UUID,
)

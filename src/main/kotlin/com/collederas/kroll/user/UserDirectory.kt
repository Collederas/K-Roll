package com.collederas.kroll.user

import java.util.UUID

interface UserDirectory {
    fun resolveDisplayName(userId: UUID): String?

    fun resolveDisplayNames(userIds: Set<UUID>): Map<UUID, String>
}

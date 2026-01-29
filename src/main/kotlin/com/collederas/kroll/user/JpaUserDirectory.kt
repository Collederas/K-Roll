package com.collederas.kroll.user

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class JpaUserDirectory(
    private val userRepository: AppUserRepository
) : UserDirectory {

    override fun resolveDisplayName(userId: UUID): String? =
        userRepository.findById(userId)
            .map { it.username }
            .orElse(null)

    override fun resolveDisplayNames(userIds: Set<UUID>): Map<UUID, String> =
        userRepository.findAllById(userIds)
            .associate { it.id to it.username }
}

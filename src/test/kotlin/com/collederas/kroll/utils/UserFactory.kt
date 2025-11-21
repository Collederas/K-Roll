package com.collederas.kroll.utils

import com.collederas.kroll.security.AuthUserDetails
import com.collederas.kroll.user.AppUser
import java.time.Instant
import java.util.UUID

object UserFactory {
    fun create(
        id: UUID = UUID.randomUUID(),
        username: String = "testuser",
        email: String = "test@example.com",
        passwordHash: String = "\$2a\$10\$fakehash",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = createdAt
    ): AppUser {
        return AppUser(
            id = id,
            username = username,
            email = email,
            passwordHash = passwordHash,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

}

object AuthUserFactory {
    fun create(
        user: AppUser
    ): AuthUserDetails
    {
        return AuthUserDetails(user)
    }
}
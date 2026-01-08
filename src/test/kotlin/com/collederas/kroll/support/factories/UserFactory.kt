package com.collederas.kroll.support.factories

import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.user.AppUser
import com.collederas.kroll.user.UserRole
import java.util.*

object UserFactory {
    fun create(
        id: UUID = UUID.randomUUID(),
        username: String = "user-${UUID.randomUUID()}",
        email: String = "user-${UUID.randomUUID()}@example.com",
        passwordHash: String = "\$2a\$10\$fakehash",
        roles: Set<UserRole> = emptySet(),
    ): AppUser =
        AppUser(
            id = id,
            username = username,
            email = email,
            passwordHash = passwordHash,
            roles = roles,
        )
}

object AuthUserFactory {
    fun create(user: AppUser): AuthUserDetails = AuthUserDetails(user)
}

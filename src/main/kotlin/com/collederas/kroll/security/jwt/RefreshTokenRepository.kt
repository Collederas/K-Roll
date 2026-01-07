package com.collederas.kroll.security.jwt

import com.collederas.kroll.user.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByToken(token: String): RefreshTokenEntity?

    /**
     * Useful for "Log out of all devices" functionality.
     */
    fun deleteAllByOwner(owner: AppUser)
}

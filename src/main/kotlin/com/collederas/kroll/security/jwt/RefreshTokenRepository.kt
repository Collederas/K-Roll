package com.collederas.kroll.security.jwt

import com.collederas.kroll.user.AppUser
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID>
{
    fun findByToken(token: String): RefreshTokenEntity?

    /**
     * Useful for "Log out of all devices" functionality.
     */
    fun deleteAllByOwner(owner: AppUser)
}


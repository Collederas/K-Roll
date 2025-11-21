package com.collederas.kroll.security.jwt

import com.collederas.kroll.user.AppUser
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity() {

    @Id
    @Column(nullable = false, unique = true)
    var id: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    lateinit var owner: AppUser

    @Column(nullable = false, unique = true)
    lateinit var token: String

    @Column(name = "expires_at", nullable = false)
    lateinit var expiresAt: Instant

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: Instant

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: Instant

    constructor(
        owner: AppUser,
        token: String,
        expiresAt: Instant,
        createdAt: Instant,
        updatedAt: Instant
    ) : this() {
        this.owner = owner
        this.token = token
        this.expiresAt = expiresAt
        this.createdAt = createdAt
        this.updatedAt = updatedAt
    }
}

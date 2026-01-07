package com.collederas.kroll.security.jwt

import com.collederas.kroll.user.AppUser
import jakarta.persistence.*
import org.hibernate.Hibernate
import java.time.Instant
import java.util.*

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id @Column(nullable = false, unique = true, updatable = false)
    var id: UUID? = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: AppUser,
    @Column(name = "token", nullable = false, unique = true) var token: String,
    @Column(name = "expires_at", nullable = false) var expiresAt: Instant,
) {
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as RefreshTokenEntity
        return id != null && id == other.id
    }

    override fun hashCode(): Int = 56789 // Constant hash code
}

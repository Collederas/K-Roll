package com.collederas.kroll.security.apikey

import com.collederas.kroll.core.environment.EnvironmentEntity
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import java.util.*

@Entity
@Table(name = "api_keys")
class ApiKeyEntity(
    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    val environment: EnvironmentEntity,
    @Column(name = "api_key_hash", nullable = false, unique = true)
    val keyHash: String,
    @Column(name = "mask", nullable = false)
    val mask: String,
    @Column(name = "expires_at")
    val expiresAt: Instant?,
) {
    @Column(name = "created_at", nullable = false)
    @CreatedDate
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }

    fun isActive(now: Instant): Boolean = expiresAt?.isAfter(now) ?: true
}

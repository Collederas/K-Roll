package com.collederas.kroll.remoteconfig.project

import com.collederas.kroll.user.AppUser
import jakarta.persistence.*
import org.hibernate.Hibernate
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "projects")
class ProjectEntity(
    @Id @Column(nullable = false, unique = true, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false)
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: AppUser,
) {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as ProjectEntity

        return id == other.id
    }

    override fun hashCode(): Int = 1839402
}

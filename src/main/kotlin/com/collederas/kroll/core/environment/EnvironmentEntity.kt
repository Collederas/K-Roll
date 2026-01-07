package com.collederas.kroll.core.environment

import com.collederas.kroll.core.project.ProjectEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "environments")
class EnvironmentEntity(
    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    val project: ProjectEntity,
    @Column(name = "name", nullable = false)
    val name: String,
) {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }
}

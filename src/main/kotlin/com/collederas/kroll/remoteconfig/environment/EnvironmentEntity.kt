package com.collederas.kroll.remoteconfig.environment

import com.collederas.kroll.remoteconfig.project.ProjectEntity
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
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)

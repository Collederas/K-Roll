package com.collederas.kroll.remoteconfig.project

import com.collederas.kroll.user.AppUser
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "projects")
class ProjectEntity (

    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    val owner: AppUser,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)

package com.collederas.kroll.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity (

    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "email", nullable = false, unique = true)
    val email: String,

    @Column(name = "username", nullable = false, unique = true)
    val username: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
)

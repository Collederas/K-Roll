package com.collederas.kroll.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "api_keys")
class ApiKeyEntity(

    @Id
    @Column(nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    val environment: EnvironmentEntity,

    @Column(name = "api_key", nullable = false, unique = true)
    val apiKey: String,

    @Column(name = "created_at", nullable = false)
    @CreatedDate
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    val updatedAt: Instant
)
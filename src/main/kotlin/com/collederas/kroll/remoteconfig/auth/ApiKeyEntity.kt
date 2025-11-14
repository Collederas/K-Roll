package com.collederas.kroll.remoteconfig.auth

import com.collederas.kroll.remoteconfig.environment.EnvironmentEntity
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

    @Column(name = "api_key", nullable = false, unique = true)
    val apiKey: String,

    @Column(name = "created_at", nullable = false)
    @CreatedDate
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    val updatedAt: Instant
)
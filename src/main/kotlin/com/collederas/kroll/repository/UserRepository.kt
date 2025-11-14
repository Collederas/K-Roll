package com.collederas.kroll.repository

import com.collederas.kroll.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun findByUsername(username: String): UserEntity?
}
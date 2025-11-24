package com.collederas.kroll.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface AppUserRepository : JpaRepository<AppUser, UUID> {
    fun findByEmail(email: String): AppUser?

    fun findByUsername(username: String): AppUser?
}

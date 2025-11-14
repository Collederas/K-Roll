package com.collederas.kroll.security

import com.collederas.kroll.user.UserEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

class CustomUserDetails(
    private val user: UserEntity
) : UserDetails {

    fun getId(): UUID = user.id
    fun getEmail(): String = user.email

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return emptyList()
    }

    override fun getPassword(): String {
        return user.passwordHash
    }

    override fun getUsername(): String {
        return user.username
    }
}
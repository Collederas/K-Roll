package com.collederas.kroll.security

import com.collederas.kroll.user.AppUser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

class AuthUserDetails(
    private val user: AppUser
) : UserDetails {

    fun getId(): UUID = user.id
    fun getEmail(): String = user.email
    fun getUser(): AppUser = user

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
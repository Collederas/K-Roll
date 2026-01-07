package com.collederas.kroll.security.identity

import com.collederas.kroll.user.AppUser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

class AuthUserDetails(
    private val user: AppUser,
) : UserDetails {
    fun getId(): UUID = user.id

    fun getEmail(): String = user.email

    fun getUser(): AppUser = user

    override fun getAuthorities(): Collection<GrantedAuthority> =
        user.roles.map {
            SimpleGrantedAuthority("ROLE_${it.name}")
        }

    override fun getPassword(): String {
        return user.passwordHash
    }

    override fun getUsername(): String {
        return user.username
    }
}

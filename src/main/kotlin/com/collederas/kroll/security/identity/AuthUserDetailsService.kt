package com.collederas.kroll.security.identity

import com.collederas.kroll.user.AppUserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthUserDetailsService(
    private val appUserRepository: AppUserRepository,
) : UserDetailsService {
    /**
     * Loads a user by username or email.
     * This is used by DaoAuthenticationProvider during the login process.
     * It attempts to find the user by email first, and if not found, attempts by username.
     *
     * @param identifier The username or email provided by the user.
     *
     */
    override fun loadUserByUsername(identifier: String): UserDetails {
        val userByEmail = appUserRepository.findByEmail(identifier)
        if (userByEmail != null) {
            return AuthUserDetails(userByEmail)
        }

        val userByUsername = appUserRepository.findByUsername(identifier)
        if (userByUsername != null) {
            return AuthUserDetails(userByUsername)
        }

        throw UsernameNotFoundException("User not found: $identifier")
    }

    fun loadUserById(id: UUID): UserDetails {
        val user =
            appUserRepository.findByIdOrNull(id)
                ?: throw UsernameNotFoundException("User not found (by ID): $id")
        return AuthUserDetails(user)
    }
}

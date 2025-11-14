package com.collederas.kroll.security

import com.collederas.kroll.user.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    /**
     * Loads a user by username or email.
     * This is used by DaoAuthenticationProvider during the login process.
     * It attempts to find the user by email first, and if not found, attempts by username.
     * This is used by DaoAuthenticationProvider (user logging-in using a form) so I can't
     * change the slightly misleading function name.
     *
     * @param identifier The username or email provided by the user.
     *
     */
    override fun loadUserByUsername(identifier: String): UserDetails {
        val userByEmail = userRepository.findByEmail(identifier)
        if (userByEmail != null) {
            return CustomUserDetails(userByEmail)
        }

        val userByUsername = userRepository.findByUsername(identifier)
        if (userByUsername != null) {
            return CustomUserDetails(userByUsername)
        }

        throw UsernameNotFoundException("User not found: $identifier")
    }

    /**
     * Loads a user by their unique, immutable ID.
     * This is the preferred method for JWT authentication.
     */
    fun loadUserById(id: UUID): UserDetails {
        val user = userRepository.findByIdOrNull(id)
            ?: throw UsernameNotFoundException("User not found (by ID): $id")
        return CustomUserDetails(user)
    }
}
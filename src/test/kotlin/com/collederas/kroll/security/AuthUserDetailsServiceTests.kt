package com.collederas.kroll.security

import com.collederas.kroll.user.AppUser
import com.collederas.kroll.user.AppUserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.time.Instant
import java.util.Optional
import java.util.UUID

class AuthUserDetailsServiceTests {
    private val appUserRepository: AppUserRepository = mockk()
    private val userDetailsService = AuthUserDetailsService(appUserRepository)

    private fun createTestUser(id: UUID): AppUser {
        return AppUser(
            id = id,
            email = "test-$id@example.com",
            username = "test-user-$id",
            passwordHash = "some-hashed-password",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    @Test
    fun `existing userId should return a valid UserDetails object`() {

        val userId = UUID.randomUUID()
        val mockUser = createTestUser(userId)
        every { appUserRepository.findById(userId) } returns Optional.of(mockUser)

        val userDetails = userDetailsService.loadUserById(userId)

        assertInstanceOf<AuthUserDetails>(userDetails)
        assertEquals(mockUser.username, userDetails.username)

        verify(exactly = 1) { appUserRepository.findById(userId) }
    }

    @Test
    fun `invalid userId should throw UsernameNotFoundException`() {
        val nonExistentId = UUID.randomUUID()

        every { appUserRepository.findById(nonExistentId) } returns Optional.empty()

        assertThrows<UsernameNotFoundException> {
            userDetailsService.loadUserById(nonExistentId)
        }
    }

    @Test
    fun `finding user by existing email should return a valid UserDetails object and stop early`() {

        val existingUser = createTestUser(UUID.randomUUID())
        val inputEmail = existingUser.email

        every { appUserRepository.findByEmail(inputEmail) } returns existingUser

        val userDetails = userDetailsService.loadUserByUsername(inputEmail)

        assertEquals(existingUser.username, userDetails.username)

        // Ensure we stopped early and didn't check username unnecessarily.
        verify(exactly = 1) { appUserRepository.findByEmail(inputEmail) }
        verify(exactly = 0) { appUserRepository.findByUsername(any()) }
    }

    @Test
    fun `finding user by existing username should return a valid UserDetails object`() {
        val existingUser = createTestUser(UUID.randomUUID())
        val inputUsername = existingUser.username

        // we don't find the email
        every { appUserRepository.findByEmail(inputUsername) } returns null

        // but we find the username
        every { appUserRepository.findByUsername(inputUsername) } returns existingUser

        val userDetails = userDetailsService.loadUserByUsername(inputUsername)

        assertEquals(existingUser.username, userDetails.username)

        verify(exactly = 1) { appUserRepository.findByEmail(inputUsername) }
        verify(exactly = 1) { appUserRepository.findByUsername(inputUsername) }
    }

    @Test
    fun `finding user by missing identifier (email or username) should throw UsernameNotFoundException`() {
        val unknownIdentifier = "ghost_user"

        every { appUserRepository.findByEmail(unknownIdentifier) } returns null
        every { appUserRepository.findByUsername(unknownIdentifier) } returns null

        assertThrows<UsernameNotFoundException> {
            userDetailsService.loadUserByUsername(unknownIdentifier)
        }

        // We checked both
        verify(exactly = 1) { appUserRepository.findByEmail(unknownIdentifier) }
        verify(exactly = 1) { appUserRepository.findByUsername(unknownIdentifier) }
    }
}
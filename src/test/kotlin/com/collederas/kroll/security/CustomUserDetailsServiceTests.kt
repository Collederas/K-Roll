package com.collederas.kroll.security

import com.collederas.kroll.user.UserEntity
import com.collederas.kroll.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.time.Instant
import java.util.Optional
import java.util.UUID

class CustomUserDetailsServiceTests {
    private val userRepository: UserRepository = mockk()
    private val userDetailsService = CustomUserDetailsService(userRepository)

    private fun createTestUser(id: UUID): UserEntity {
        return UserEntity(
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
        every { userRepository.findById(userId) } returns Optional.of(mockUser)

        val userDetails = userDetailsService.loadUserById(userId)

        assertInstanceOf<CustomUserDetails>(userDetails)
        Assertions.assertEquals(mockUser.username, userDetails.username)

        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `invalid userId should throw UsernameNotFoundException`() {
        val nonExistentId = UUID.randomUUID()

        every { userRepository.findById(nonExistentId) } returns Optional.empty()

        assertThrows<UsernameNotFoundException> {
            userDetailsService.loadUserById(nonExistentId)
        }
    }

    @Test
    fun `finding user by existing email should return a valid UserDetails object and stop early`() {

        val existingUser = createTestUser(UUID.randomUUID())
        val inputEmail = existingUser.email

        every { userRepository.findByEmail(inputEmail) } returns existingUser

        val userDetails = userDetailsService.loadUserByUsername(inputEmail)

        Assertions.assertEquals(existingUser.username, userDetails.username)

        // Ensure we stopped early and didn't check username unnecessarily.
        verify(exactly = 1) { userRepository.findByEmail(inputEmail) }
        verify(exactly = 0) { userRepository.findByUsername(any()) }
    }

    @Test
    fun `finding user by existing username should return a valid UserDetails object`() {
        val existingUser = createTestUser(UUID.randomUUID())
        val inputUsername = existingUser.username

        // we don't find the email
        every { userRepository.findByEmail(inputUsername) } returns null

        // but we find the username
        every { userRepository.findByUsername(inputUsername) } returns existingUser

        val userDetails = userDetailsService.loadUserByUsername(inputUsername)

        Assertions.assertEquals(existingUser.username, userDetails.username)

        verify(exactly = 1) { userRepository.findByEmail(inputUsername) }
        verify(exactly = 1) { userRepository.findByUsername(inputUsername) }
    }

    @Test
    fun `finding user by missing identifier (email or username) should throw UsernameNotFoundException`() {
        val unknownIdentifier = "ghost_user"

        every { userRepository.findByEmail(unknownIdentifier) } returns null
        every { userRepository.findByUsername(unknownIdentifier) } returns null

        assertThrows<UsernameNotFoundException> {
            userDetailsService.loadUserByUsername(unknownIdentifier)
        }

        // We checked both
        verify(exactly = 1) { userRepository.findByEmail(unknownIdentifier) }
        verify(exactly = 1) { userRepository.findByUsername(unknownIdentifier) }
    }
}
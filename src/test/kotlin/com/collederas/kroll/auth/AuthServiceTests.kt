package com.collederas.kroll.auth

import com.collederas.kroll.security.jwt.JwtTokenService
import com.collederas.kroll.user.AppUser
import com.collederas.kroll.user.AuthService
import com.collederas.kroll.user.AuthUserDetails
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.time.Instant
import java.util.*

class AuthServiceTests {
    private val authManager: AuthenticationManager = mockk()
    private val jwtTokenService: JwtTokenService = mockk()

    private val authService = AuthService(authManager, jwtTokenService)

    @AfterEach
    fun tearDown() = clearAllMocks()

    private fun fakePrincipal(): AuthUserDetails {
        val user = AppUser(
            id = UUID.randomUUID(),
            email = "user@example.com",
            username = "testUser",
            passwordHash = "hash",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return AuthUserDetails(user)
    }

    private fun authenticationWith(principal: Any): Authentication {
        val auth = mockk<Authentication>()
        every { auth.principal } returns principal
        return auth
    }

    @Test
    fun `valid login returns token`() {
        val principal = fakePrincipal()
        val token = "jwt.token"

        every { authManager.authenticate(any()) } returns authenticationWith(principal)
        every { jwtTokenService.generateToken(principal.getId(), "testUser") } returns token

        val result = authService.login("user@example.com", "password")

        assertEquals(token, result)
        verify(exactly = 1) {
            authManager.authenticate(
                withArg { auth ->
                    assert(auth is UsernamePasswordAuthenticationToken)
                    assertEquals("user@example.com", auth.principal)
                    assertEquals("password", auth.credentials)
                }
            )
        }
    }

    @Test
    fun `bad credentials propagate`() {
        every { authManager.authenticate(any()) } throws BadCredentialsException(
            "bad")

        assertThrows(BadCredentialsException::class.java) {
            authService.login("user@example.com", "wrong")
        }
    }

    @Test
    fun `unknown user propagates`() {
        every { authManager.authenticate(any()) } throws UsernameNotFoundException(
            "user does not exist")

        assertThrows(UsernameNotFoundException::class.java) {
            authService.login("nope@example.com", "pwd")
        }
    }

    @Test
    fun `non CustomUserDetails principal causes ClassCastException`() {
        every { authManager.authenticate(any()) } returns authenticationWith("not-custom-user")

        assertThrows(ClassCastException::class.java) {
            authService.login("user@example.com", "pwd")
        }
    }
}
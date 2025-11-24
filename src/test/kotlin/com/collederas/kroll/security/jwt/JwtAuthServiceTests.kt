package com.collederas.kroll.security.jwt

import com.collederas.kroll.security.AuthUserDetails
import com.collederas.kroll.utils.AuthUserFactory
import com.collederas.kroll.utils.UserFactory
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UsernameNotFoundException

class JwtAuthServiceTests {
    private val authManager: AuthenticationManager = mockk()
    private val jwtTokenService: JwtTokenService = mockk()
    private val refreshTokenService: RefreshTokenService = mockk()

    private val authService =
        JwtAuthService(
            authManager,
            jwtTokenService,
            refreshTokenService,
        )

    @AfterEach
    fun tearDown() = clearAllMocks()

    private fun fakePrincipal(username: String?): AuthUserDetails {
        val username = username ?: "user@example.com"
        val principal = UserFactory.create(username = username)
        return AuthUserFactory.create(principal)
    }

    private fun authenticationWith(principal: Any): Authentication {
        val auth = mockk<Authentication>()
        every { auth.principal } returns principal
        return auth
    }

    @Test
    fun `valid login returns access and refresh token`() {
        val principal = fakePrincipal("testUser")
        val accessToken = "accessToken"
        val refreshToken = "refreshToken"

        every { authManager.authenticate(any()) } returns authenticationWith(principal)
        every { jwtTokenService.generateToken(principal.getId(), "testUser") } returns accessToken
        every { refreshTokenService.rotateAllTokensFor(principal.getUser()) } returns refreshToken

        val (resultAccess, resultRefresh) = authService.login("user@example.com", "password")

        assertEquals(accessToken, resultAccess)
        assertEquals(refreshToken, resultRefresh)

        verify(exactly = 1) {
            authManager.authenticate(
                withArg { auth ->
                    assert(auth is UsernamePasswordAuthenticationToken)
                    assertEquals("user@example.com", auth.principal)
                    assertEquals("password", auth.credentials)
                },
            )
        }
    }

    @Test
    fun `refreshToken returns new access and refresh`() {
        val user = UserFactory.create()
        val newRefresh = "newRefresh"
        val newAccess = "newAccess"

        every { refreshTokenService.rotateFromRefresh("old") } returns (user to newRefresh)
        every { jwtTokenService.generateToken(user.id, user.username) } returns newAccess

        val (access, refresh) = authService.refreshToken("old")

        assert(access == newAccess)
        assert(refresh == newRefresh)
    }

    @Test
    fun `bad credentials propagate`() {
        every { authManager.authenticate(any()) } throws
            BadCredentialsException(
                "bad",
            )

        assertThrows(BadCredentialsException::class.java) {
            authService.login("user@example.com", "wrong")
        }
    }

    @Test
    fun `unknown user propagates`() {
        every { authManager.authenticate(any()) } throws
            UsernameNotFoundException(
                "user does not exist",
            )

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

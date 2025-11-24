package com.collederas.kroll.security.jwt

import com.collederas.kroll.security.AuthUserDetailsService
import com.collederas.kroll.security.SecurityConfig
import com.collederas.kroll.utils.AuthUserFactory
import com.collederas.kroll.utils.UserFactory
import com.ninjasquad.springmockk.MockkBean // Requires springmockk dependency
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(JwtAuthController::class)
@Import(SecurityConfig::class, JwtAuthFilter::class)
class JwtAuthControllerTests {
    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var jwtTokenService: JwtTokenService

    @MockkBean(relaxed = true)
    private lateinit var authService: JwtAuthService

    @MockkBean(relaxed = true)
    private lateinit var userDetailsService: AuthUserDetailsService

    @MockkBean(relaxed = true)
    private lateinit var jwtAuthEntryPoint: JwtAuthEntryPoint

    @Test
    fun `login with valid credentials returns valid tokens`() {
        val identifier = "user@example.com"
        val password = "password"
        every { authService.login(identifier, password) } returns ("access" to "refresh")

        mockMvc
            .post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"identifier":"$identifier", "password":"$password"}"""
            }
            .andExpect {
                status { isOk() }
                jsonPath("$.access") { value("access") }
            }
    }

    @Test
    fun `logout returns 204`() {
        val principalUser = UserFactory.create()
        val principal = AuthUserFactory.create(principalUser)

        mockMvc.post("/auth/logout") {
            with(user(principal))
        }.andExpect {
            status { isNoContent() }
        }

        verify { authService.revokeTokenFor(any()) }
    }

    @Test
    fun `login with invalid credentials returns 401`() {
        every { authService.login(any(), any()) } throws BadCredentialsException("Bad creds")

        mockMvc
            .post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"identifier":"u", "password":"p"}"""
            }
            .andExpect { status { isUnauthorized() } }
    }
}

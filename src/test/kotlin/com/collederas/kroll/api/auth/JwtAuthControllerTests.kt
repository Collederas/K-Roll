package com.collederas.kroll.api.auth

import com.collederas.kroll.api.admin.auth.AdminAuthController
import com.collederas.kroll.exceptions.InvalidCredentialsException
import com.collederas.kroll.security.SecurityConfig
import com.collederas.kroll.security.jwt.JwtSecurityConfig
import com.collederas.kroll.security.jwt.authentication.JwtAuthService
import com.collederas.kroll.support.TestSecurityMocks
import com.collederas.kroll.support.factories.AuthUserFactory
import com.collederas.kroll.support.factories.UserFactory
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(AdminAuthController::class)
@Import(SecurityConfig::class, JwtSecurityConfig::class, TestSecurityMocks::class)
@ActiveProfiles("test")
class JwtAuthControllerTests {
    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var authService: JwtAuthService

    @Test
    fun `login with valid credentials returns valid tokens`() {
        val identifier = "user@example.com"
        val password = "password"
        every { authService.login(identifier, password) } returns ("access" to "refresh")

        mvc
            .post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"identifier":"$identifier", "password":"$password"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.access") { value("access") }
            }

        verify(exactly = 1) { authService.login(identifier, password) }
    }

    @Test
    fun `logout returns 204`() {
        val user = UserFactory.create()
        val authUser = AuthUserFactory.create(user)

        every { authService.revokeTokenFor(user) } just Runs

        mvc
            .post("/auth/logout") {
                with(SecurityMockMvcRequestPostProcessors.user(authUser))
            }.andExpect {
                status { isNoContent() }
            }

        verify(exactly = 1) { authService.revokeTokenFor(user) }
    }

    @Test
    fun `login with invalid credentials returns 401`() {
        every { authService.login(any(), any()) } throws
            InvalidCredentialsException()

        mvc
            .post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"identifier":"u", "password":"p"}"""
            }.andExpect { status { isBadRequest() } }
    }
}

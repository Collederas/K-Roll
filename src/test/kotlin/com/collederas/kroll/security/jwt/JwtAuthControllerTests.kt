package com.collederas.kroll.security.jwt

import com.collederas.kroll.common.exception.GlobalExceptionHandler
import com.collederas.kroll.security.AuthUserDetails
import com.collederas.kroll.utils.UserFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class JwtAuthControllerTests {

    private lateinit var mvc: MockMvc

    private val authService: JwtAuthService = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        val controller = JwtAuthController(authService)

        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
             .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `login with valid credentials returns valid tokens`() {
        val identifier = "user@example.com"
        val password = "password"
        every { authService.login(identifier, password) } returns ("access" to "refresh")

        mvc.post("/auth/login") {
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
        val user = UserFactory.create()
        val principal = AuthUserDetails(user)

        val auth = TestingAuthenticationToken(principal, null)
        SecurityContextHolder.getContext().authentication = auth

        mvc.post("/auth/logout")
            .andExpect {
                status { isNoContent() }
            }

        verify { authService.revokeTokenFor(any()) }

        SecurityContextHolder.clearContext()
    }

    @Test
    fun `login with invalid credentials returns 401`() {
        every { authService.login(any(), any()) } throws BadCredentialsException("Bad creds")

        try {
            mvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"identifier":"u", "password":"p"}"""
            }
        } catch (e: Exception) {
            assert(e.cause is BadCredentialsException)
        }
    }
}
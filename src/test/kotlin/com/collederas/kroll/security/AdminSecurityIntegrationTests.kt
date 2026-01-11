package com.collederas.kroll.security

import com.collederas.kroll.api.ProjectController
import com.collederas.kroll.api.auth.JwtAuthController
import com.collederas.kroll.core.project.ProjectService
import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.security.identity.AuthUserDetailsService
import com.collederas.kroll.security.jwt.JwtSecurityConfig
import com.collederas.kroll.security.jwt.JwtTokenService
import com.collederas.kroll.security.jwt.authentication.JwtAuthFilter
import com.collederas.kroll.security.jwt.authentication.JwtAuthService
import com.collederas.kroll.support.TestSecurityMocks
import com.collederas.kroll.user.AppUser
import com.collederas.kroll.user.UserRole
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.*

@Import(
    SecurityConfig::class,
    JwtSecurityConfig::class,
    TestSecurityMocks::class,
    JwtAuthFilter::class,
)
@WebMvcTest(JwtAuthController::class, ProjectController::class)
@ActiveProfiles("test")
class AdminSecurityIntegrationTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var jwtService: JwtTokenService

    @MockkBean
    lateinit var userDetailsService: AuthUserDetailsService

    @MockkBean
    lateinit var authService: JwtAuthService

    @MockkBean
    lateinit var projectService: ProjectService

    @Test
    fun `protected route - invalid token returns 401`() {
        every { jwtService.validateAndGetUserId("BAD") } returns null

        mvc
            .get("/api/projects") {
                header("Authorization", "Bearer BAD")
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `protected route - invalid token sets authenticate header`() {
        every { jwtService.validateAndGetUserId("BAD") } returns null

        mvc
            .get("/api/projects") {
                header("Authorization", "Bearer BAD")
            }.andExpect {
                status { isUnauthorized() }
                header {
                    string("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                }
            }
    }

    @Test
    fun `protected route - valid ADMIN token returns 200`() {
        val adminUser = createMockUser(UserRole.ADMIN)
        mockUserAuthentication(adminUser, "jwt.admin")
        every { projectService.list(adminUser.getId()) } returns emptyList()

        mvc
            .get("/api/projects") {
                header("Authorization", "Bearer jwt.admin")
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `protected route - logout requires authentication`() {
        mvc
            .post("/auth/logout")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `public route - login accessible without token`() {
        every { authService.login(any(), any()) } throws
            BadCredentialsException("Invalid credentials")

        val loginPayload = mapOf("identifier" to "x", "password" to "y")

        mvc
            .post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(loginPayload)
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `public route - refresh accessible without access token`() {
        every { authService.refreshToken(any()) } returns
            ("newAccess" to "newRefresh")

        val refreshPayload = mapOf("refresh" to "valid-refresh-token")

        mvc
            .post("/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(refreshPayload)
            }.andExpect {
                status { isOk() }
            }
    }

    private fun createMockUser(role: UserRole): AuthUserDetails {
        val id = UUID.randomUUID()
        return AuthUserDetails(
            AppUser(
                id = id,
                email = "test@example.com",
                username = "test",
                passwordHash = "xyz",
                roles = setOf(role),
            ),
        )
    }

    private fun mockUserAuthentication(
        user: AuthUserDetails,
        token: String,
    ) {
        every { jwtService.validateAndGetUserId(token) } returns user.getId()
        every { userDetailsService.loadUserById(user.getId()) } returns user
    }
}

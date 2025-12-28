package com.collederas.kroll.security

import com.collederas.kroll.security.jwt.JwtTokenService
import com.collederas.kroll.security.user.AuthUserDetails
import com.collederas.kroll.security.user.AuthUserDetailsService
import com.collederas.kroll.user.AppUser
import com.collederas.kroll.user.UserRole
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.*


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityIntegrationTests {

    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    lateinit var jwtService: JwtTokenService

    @MockkBean
    lateinit var userDetailsService: AuthUserDetailsService

    @MockkBean
    lateinit var authService: AuthenticationService

    @Test
    fun `protected route - invalid token returns 401`() {
        val scope = every { jwtService.validateAndGetUserId("BAD") } returns null

        mvc.get("/admin/projects") {
            header("Authorization", "Bearer BAD")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `protected route - invalid token sets correct authenticate header`() {
        every { jwtService.validateAndGetUserId("BAD") } returns null

        mvc.get("/admin/projects") {
            header("Authorization", "Bearer BAD")
        }.andExpect {
            status { isUnauthorized() }
            header { string("WWW-Authenticate", "Bearer error=\"invalid_token\"") }
        }
    }

    @Test
    fun `protected route - valid token returns 200`() {
        val id = UUID.randomUUID()
        val details = AuthUserDetails(
            AppUser(
                id = id,
                email = "test@example.com",
                username = "test",
                passwordHash = "xyz",
                roles = setOf(UserRole.ADMIN)
            )
        )

        every { userDetailsService.loadUserById(id) } returns details
        every { jwtService.validateAndGetUserId("jwt.token") } returns id

        mvc.get("/admin/projects") {
            header("Authorization", "Bearer jwt.token")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `protected route - logout requires authentication`() {
        mvc.post("/auth/logout")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `public route - login accessible without token`() {
        every { authService.login(any(), any()) } throws BadCredentialsException("Invalid credentials")

        mvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"identifier":"x","password":"y"}"""
        }
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `public route - refresh accessible without access token`() {
        every { authService.refreshToken(any()) } returns ("newAccess" to "newRefresh")

        mvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refresh":"valid-refresh-token"}"""
        }
            .andExpect {
                status { isOk() }
            }
    }
}

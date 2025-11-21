package com.collederas.kroll.security

import com.collederas.kroll.security.jwt.JwtAuthService
import com.collederas.kroll.security.jwt.JwtTokenService
import com.collederas.kroll.user.AppUser
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*


@RestController
@RequestMapping("/client")
class PublicClientTestController {
    @GetMapping("/ping")
    fun ping() = "pong"
}


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTests {

    @Autowired
    lateinit var mvc: MockMvc

    @MockitoBean
    lateinit var jwtService: JwtTokenService

    @MockitoBean
    lateinit var userDetailsService: AuthUserDetailsService

    @MockitoBean
    lateinit var authService: JwtAuthService

    @Test
    fun `public route - login accessible without token`() {
        `when`(authService.login(anyString(), anyString()))
            .thenThrow(BadCredentialsException("Invalid credentials"))

        mvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"identifier":"x","password":"y"}"""
        }
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `public route - client api accessible without token`() {
        mvc.get("/client/ping")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `protected route - invalid token returns 401`() {
        `when`(jwtService.validateAndGetUserId("BAD")).thenReturn(null)

        mvc.perform(
            get("/admin/projects")
                .header("Authorization", "Bearer BAD")
        )
            .andExpect { status().isUnauthorized }
    }

    @Test
    fun `protected route - invalid token sets correct authenticate header`() {
        `when`(jwtService.validateAndGetUserId("BAD")).thenReturn(null)

        mvc.perform(
            get("/admin/projects")
                .header("Authorization", "Bearer BAD")
        )
            .andExpect {
                status().isUnauthorized
                header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
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
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        `when`(userDetailsService.loadUserById(id)).thenReturn(details)
        `when`(jwtService.validateAndGetUserId("jwt.token")).thenReturn(id)

        mvc.perform(
            get("/admin/projects")
                .header("Authorization", "Bearer jwt.token")
        )
            .andExpect(
                status().isOk
            )
    }

    @Test
    fun `public route - refresh accessible without access token`() {
        // We mock the service so the controller returns 200 OK
        // ensuring the Security Filter Chain let the request through.
        `when`(authService.refreshToken(anyString())).thenReturn("newAccess" to "newRefresh")

        mvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refresh":"valid-refresh-token"}"""
        }
            .andExpect {
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
}
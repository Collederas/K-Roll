package com.collederas.kroll.auth

import com.collederas.kroll.security.jwt.JwtAuthFilter
import com.collederas.kroll.user.AuthController
import com.collederas.kroll.user.AuthService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests {

    @Autowired
    lateinit var mvc: MockMvc

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var jwtAuthFilter: JwtAuthFilter

    @Test
    fun `login with valid credentials returns 200 and token`() {
        val identifier = "user@example.com"
        val password = "password"

        `when`(authService.login(identifier, password))
            .thenReturn("jwt.token")
        mvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"identifier":"$identifier", "password":"$password"}
            """
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.token") { value("jwt.token") }
            }
    }

    @Test
    fun `login with invalid credentials returns 401 and no token`() {
        `when`(authService.login(anyString(), anyString()))
            .thenThrow(BadCredentialsException("bad creds"))

        mvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"identifier":"user@example.com", "password":"wrong"}
            """
        }
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `login with non existent user returns 401 and no token`() {
        `when`(authService.login(anyString(), anyString()))
            .thenThrow(UsernameNotFoundException("user does not exist"))

        mvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                    {"identifier":"invalid@example.com", "password":"password"}
                """
        }
        .andExpect {
            status { isUnauthorized() }
        }
    }
}


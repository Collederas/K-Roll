package com.collederas.kroll.security

import com.collederas.kroll.config.SecurityTestConfig
import com.collederas.kroll.security.jwt.JwtTokenService
import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.security.identity.AuthUserDetailsService
import com.collederas.kroll.support.factories.UserFactory
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityTestConfig::class)
@ActiveProfiles("test")
class JwtAuthFilterTests {
    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    lateinit var jwtService: JwtTokenService

    @MockkBean
    lateinit var userDetailsService: AuthUserDetailsService

    private fun userDetails(id: UUID): AuthUserDetails {
        val user = UserFactory.create(id = id)
        return AuthUserDetails(user)
    }

    @Test
    fun `valid token populates security context`() {
        val token = "valid.jwt"
        val userId = UUID.randomUUID()
        val details = userDetails(userId)

        every { jwtService.validateAndGetUserId(token) } returns userId
        every { userDetailsService.loadUserById(userId) } returns details

        mvc.perform(get("/test/auth/whoami").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.principal.user.id").value(userId.toString()))
    }

    @Test
    fun `missing Authorization header leaves context empty`() {
        mvc.perform(get("/test/auth/whoami"))
            .andExpect(status().isOk)
            .andExpect(
                jsonPath("$.authenticated").value(false),
            )
    }

    @Test
    fun `invalid scheme leaves context empty`() {
        mvc.perform(get("/test/auth/whoami").header("Authorization", "Basic xyz"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.authenticated").value(false))
    }

    @Test
    fun `existing authentication is not overwritten`() {
        val token = "valid.jwt"
        val userId = UUID.randomUUID()
        val otherUser = userDetails(userId)

        every { jwtService.validateAndGetUserId(token) } returns userId
        every { userDetailsService.loadUserById(userId) } returns otherUser

        // even if a valid token is passed, we don't want to authenticate it
        mvc.perform(
            get("/test/auth/whoami")
                .header("X-Simulate-PreAuth", "true")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.authorities[0]").value("ROLE_PREAUTH"))
    }
}

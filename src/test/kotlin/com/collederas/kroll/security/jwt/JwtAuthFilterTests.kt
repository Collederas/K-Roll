package com.collederas.kroll.security.jwt

import com.collederas.kroll.security.TestJwtSecurityConfig
import com.collederas.kroll.security.apikey.authentication.ApiKeyAuthenticationFilter
import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.security.identity.AuthUserDetailsService
import com.collederas.kroll.support.controllers.JwtAuthIntrospectionController
import com.collederas.kroll.support.factories.UserFactory
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(
    controllers = [JwtAuthIntrospectionController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [ApiKeyAuthenticationFilter::class],
        ),
    ],
)
@Import(TestJwtSecurityConfig::class)
class JwtAuthFilterTests {
    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    lateinit var jwtService: JwtTokenService

    @MockkBean
    lateinit var userDetailsService: AuthUserDetailsService

    private val testEndpoint = "/test/auth/jwt/whoami"

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

        mvc
            .perform(get(testEndpoint).header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.principal.user.id").value(userId.toString()))
    }

    @Test
    fun `missing Authorization header leaves context empty`() {
        mvc
            .perform(get(testEndpoint))
            .andExpect(status().isOk)
            .andExpect(
                jsonPath("$.authenticated").value(false),
            )
    }

    @Test
    fun `invalid scheme leaves context empty`() {
        mvc
            .perform(get(testEndpoint).header("Authorization", "Basic xyz"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.authenticated").value(false))
    }

    @Test
    fun `existing authentication is not overwritten`() {
        val token = "valid.jwt"
        val userId = UUID.randomUUID()
        val otherUser = userDetails(userId)

        // pre-existing authentication
        val preAuth =
            UsernamePasswordAuthenticationToken(
                "preauth",
                null,
                listOf(SimpleGrantedAuthority("ROLE_PREAUTH")),
            )
        SecurityContextHolder.getContext().authentication = preAuth

        every { jwtService.validateAndGetUserId(token) } returns userId
        every { userDetailsService.loadUserById(userId) } returns otherUser

        // even if a valid token is passed, we don't want to authenticate it
        mvc
            .perform(
                get(testEndpoint)
                    .header("X-Simulate-PreAuth", "true")
                    .header("Authorization", "Bearer $token"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.authorities[0]").value("ROLE_PREAUTH"))
    }
}

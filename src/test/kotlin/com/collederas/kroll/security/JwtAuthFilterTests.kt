package com.collederas.kroll.security

import com.collederas.kroll.security.token.JwtTokenService
import com.collederas.kroll.security.CustomUserDetailsService
import com.collederas.kroll.user.UserEntity
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*


@RestController
class FilterTestController {
    @GetMapping("/open")
    fun testEndpoint() = "OK"

    @GetMapping("/protected")
    fun protected(authentication: Authentication?): String {
        val p = authentication?.principal as? CustomUserDetails ?: return "none"
        return p.getId().toString()
    }

    @GetMapping("/echo-auth")
    fun echoAuth(authentication: Authentication?) = authentication?.name ?: "none"
}

@TestConfiguration
class TestSecurityConfig {
    @Bean
    fun filterChain(
        http: HttpSecurity,
        jwtAuthFilter: JwtAuthFilter
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}

@WebMvcTest(FilterTestController::class)
@Import(TestSecurityConfig::class, JwtAuthFilter::class)
@ActiveProfiles("test")
class JwtAuthFilterTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var jwtService: JwtTokenService

    @MockitoBean
    lateinit var userDetailsService: CustomUserDetailsService

    private fun userDetails(id: UUID): CustomUserDetails {
        val user = UserEntity(
            id = id,
            email = "test@example.com",
            username = "testuser",
            passwordHash = "hash",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return CustomUserDetails(user)
    }

    @Test
    fun `valid token populates security context`() {
        val token = "valid.jwt"
        val userId = UUID.randomUUID()
        val details = userDetails(userId)

        `when`(jwtService.validateAndGetUserId(token)).thenReturn(userId)
        `when`(userDetailsService.loadUserById(userId)).thenReturn(details)

        mockMvc.perform(get("/protected").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(content().string(userId.toString()))
    }

    @Test
    fun `missing Authorization header leaves context empty`() {
        mockMvc.perform(get("/protected"))
            .andExpect(status().isOk)
            .andExpect(content().string("none"))
    }

    @Test
    fun `invalid scheme leaves context empty`() {
        mockMvc.perform(get("/protected").header("Authorization", "Basic xyz"))
            .andExpect(status().isOk)
            .andExpect(content().string("none"))
    }

    @Test
    fun `invalid token leaves context empty`() {
        `when`(jwtService.validateAndGetUserId("bad")).thenReturn(null)

        mockMvc.perform(get("/protected").header("Authorization", "Bearer bad"))
            .andExpect(status().isOk)
            .andExpect(content().string("none"))
    }

    @Test
    fun `existing authentication is not overwritten`() {
        val original = UsernamePasswordAuthenticationToken("existing", null, emptyList())
        val token = "valid.jwt"
        val userId = UUID.randomUUID()
        val otherUser = userDetails(userId)

        `when`(jwtService.validateAndGetUserId(token)).thenReturn(userId)
        `when`(userDetailsService.loadUserById(userId)).thenReturn(otherUser)

        // even if a valid token is passed, we don't want to authenticate it
        mockMvc.perform(
            get("/echo-auth")
                .with(authentication(original))
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(content().string("existing"))
    }
}

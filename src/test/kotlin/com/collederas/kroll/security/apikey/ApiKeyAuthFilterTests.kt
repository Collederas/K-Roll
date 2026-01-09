package com.collederas.kroll.security.apikey

import com.collederas.kroll.security.apikey.authentication.ApiKeyAuthenticationProvider
import com.collederas.kroll.security.apikey.authentication.ApiKeyAuthenticationToken
import com.collederas.kroll.security.jwt.authentication.JwtAuthFilter
import com.collederas.kroll.support.TestApiKeySecurityConfig
import com.collederas.kroll.support.controllers.ApiKeyAuthIntrospectionController
import com.collederas.kroll.support.factories.ApiKeyFactory
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@WebMvcTest(
    controllers = [ApiKeyAuthIntrospectionController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [JwtAuthFilter::class],
        ),
    ],
)
@Import(
    TestApiKeySecurityConfig::class,
)
open class ApiKeyAuthFilterTests {
    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var apiKeyRepository: ApiKeyRepository

    @MockkBean
    private lateinit var apiKeyAuthenticationProvider: ApiKeyAuthenticationProvider

    private val testEndpoint = "/test/auth/apikey/whoami"
    private val authHeader = "X-Api-Key"

    fun makeToken(
        rawKey: String,
        authenticated: Boolean,
    ): ApiKeyAuthenticationToken {
        ApiKeyAuthenticationToken(rawKey)
        val token = ApiKeyAuthenticationToken(rawKey)
        val tokenAuth =
            ApiKeyAuthenticationToken(
                "principal",
                null,
                listOf(SimpleGrantedAuthority("ROLE_GAME_CLIENT")),
            )
        return if (authenticated) {
            tokenAuth
        } else {
            token
        }
    }

    @Test
    fun `valid api key sets SecurityContext with correct role`() {
        val apiKey = ApiKeyFactory.create()

        val authdToken = makeToken(apiKey.keyHash, true)
        val unauthdToken = makeToken(apiKey.keyHash, false)

        every { apiKeyRepository.findByKeyHash(any()) } returns apiKey
        every {
            apiKeyAuthenticationProvider.supports(ApiKeyAuthenticationToken::class.java)
        } returns true
        every { apiKeyAuthenticationProvider.authenticate(unauthdToken) } returns authdToken

        mvc
            .get(testEndpoint) {
                header(authHeader, apiKey.keyHash)
            }.andExpect {
                status { isOk() }
                jsonPath("$.authenticated").value(true)
                jsonPath("$.authorities[0]").value("ROLE_GAME_CLIENT")
            }
    }

    @Test
    fun `missing api key does not authenticate`() {
        mvc
            .get("/test/auth/apikey/whoami")
            .andExpect {
                status { isOk() }
                jsonPath("$.authenticated").value(false)
            }
    }

    @Test
    fun `api key does not override existing authentication`() {
        val apiKey = ApiKeyFactory.create()

        val token = makeToken(apiKey.keyHash, true)

        // pre-existing authentication
        val preAuth =
            UsernamePasswordAuthenticationToken(
                "preauth",
                null,
                listOf(SimpleGrantedAuthority("ROLE_PREAUTH")),
            )
        SecurityContextHolder.getContext().authentication = preAuth

        every { apiKeyRepository.findByKeyHash(any()) } returns apiKey
        every {
            apiKeyAuthenticationProvider.authenticate(
                ofType<ApiKeyAuthenticationToken>(),
            )
        } returns token
        every {
            apiKeyAuthenticationProvider.supports(ApiKeyAuthenticationToken::class.java)
        } returns true

        mvc
            .get("/test/auth/apikey/whoami") {
                header(authHeader, "valid-key")
                header("X-Simulate-PreAuth", "true")
            }.andExpect { jsonPath("$.authorities[0]").value("ROLE_PREAUTH") }

        verify(exactly = 0) {
            apiKeyAuthenticationProvider.authenticate(any())
        }
    }
}

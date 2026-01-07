package com.collederas.kroll.security.apikey

import com.collederas.kroll.config.SecurityTestConfig
import com.collederas.kroll.security.apikey.dto.ApiKeyAuthResult
import com.collederas.kroll.security.jwt.authentication.JwtAuthFilter
import com.collederas.kroll.support.controllers.AuthIntrospectionController
import com.collederas.kroll.support.factories.ApiKeyFactory
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.util.*

@WebMvcTest(
    controllers = [AuthIntrospectionController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [JwtAuthFilter::class],
        ),
    ],
)
@Import(SecurityTestConfig::class)
open class ApiKeyAuthFilterTests {
    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var apiKeyRepository: ApiKeyRepository

    @MockkBean
    lateinit var apiKeyService: ApiKeyService

    private val testEndpoint = "/test/auth/whoami"
    private val authHeader = "X-Api-Key"

    @Test
    fun `valid api key sets SecurityContext with correct role`() {
        val rawKey = "rk_test"
        val apiKey = ApiKeyFactory.create()

        every { apiKeyRepository.findByKeyHash(any()) } returns apiKey
        every { apiKeyService.validate(rawKey) } returns
            ApiKeyAuthResult(
                environmentId = UUID.randomUUID(),
                apiKeyId = UUID.randomUUID(),
                roles = listOf("ROLE_GAME_CLIENT"),
            )

        mvc
            .get(testEndpoint) {
                header(authHeader, rawKey)
            }.andExpect {
                status { isOk() }
                jsonPath("$.authenticated").value(true)
                jsonPath("$.authorities[0]").value("ROLE_GAME_CLIENT")
            }
    }

    @Test
    fun `missing api key does not authenticate`() {
        mvc
            .get("/test/auth/whoami")
            .andExpect {
                status { isOk() }
                jsonPath("$.authenticated").value(false)
            }
    }

    @Test
    fun `api key does not override existing authentication`() {
        mvc
            .get("/test/auth/whoami") {
                header(authHeader, "valid-key")
                header("X-Simulate-PreAuth", "true")
            }.andExpect { jsonPath("$.authorities[0]").value("ROLE_PREAUTH") }
    }
}

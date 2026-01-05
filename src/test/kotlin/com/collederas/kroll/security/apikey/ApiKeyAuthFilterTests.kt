package com.collederas.kroll.security.apikey

import com.collederas.kroll.config.SecurityTestConfig
import com.collederas.kroll.support.factories.ApiKeyFactory
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityTestConfig::class)
open class ApiKeyAuthFilterTests {
    @Autowired
    lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var apiKeyRepository: ApiKeyRepository

    private val testEndpoint = "/test/auth/whoami"
    private val authHeader = "X-Api-Key"

    @Test
    fun `valid api key sets SecurityContext with correct role`() {
        val rawKey = "rk_test"
        every { apiKeyRepository.findByKeyHash(any()) } returns ApiKeyFactory.create()

        mvc.get(testEndpoint) {
            header(authHeader, rawKey)
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.authenticated").value(true)
                jsonPath("$.authorities[0]").value("ROLE_GAME_CLIENT")
            }
    }

    @Test
    fun `missing api key does not authenticate`() {
        mvc.get("/test/auth/whoami")
            .andExpect {
                status { isOk() }
                jsonPath("$.authenticated").value(false)
            }
    }

    @Test
    fun `api key does not override existing authentication`() {
        mvc.get("/test/auth/whoami") {
            header(authHeader, "valid-key")
            header("X-Simulate-PreAuth", "true")
        }
            .andExpect { jsonPath("$.authorities[0]").value("ROLE_PREAUTH") }
    }
}

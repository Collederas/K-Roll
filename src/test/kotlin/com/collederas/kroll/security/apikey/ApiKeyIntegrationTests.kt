package com.collederas.kroll.security.apikey

import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.Duration
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiKeyIntegrationTest {

    @Autowired
    lateinit var envFactory: PersistedEnvironmentFactory

    @Autowired
    lateinit var apiKeyService: ApiKeyService

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun `deleted api key is immediately invalidated`() {
        val env = envFactory.create()

        val created = apiKeyService.create(
            env.id,
            Instant.now().plus(Duration.ofDays(1))
        )

        mvc.get("/test/auth/whoami") {
            header("X-Api-Key", created.key)
        }.andExpect {
            jsonPath("$.authenticated").value(true)
        }

        apiKeyService.delete(created.id)

        mvc.get("/test/auth/whoami") {
            header("X-Api-Key", created.key)
        }.andExpect {
            jsonPath("$.authenticated").value(false)
        }
    }

    @Test
    fun `expired api key does not authenticate`() {
        val env = envFactory.create()

        val created = apiKeyService.create(
            env.id,
            Instant.now().minus(Duration.ofSeconds(1))
        )

        mvc.get("/test/auth/whoami") {
            header("X-Api-Key", created.key)
        }.andExpect {
            jsonPath("$.authenticated").value(false)
        }
    }

    @Test
    fun `unknown api key does not authenticate`() {
        envFactory.create() // ensures DB is not empty

        mvc.get("/test/auth/whoami") {
            header("X-Api-Key", "rk_definitely_not_existing")
        }.andExpect {
            jsonPath("$.authenticated").value(false)
        }
    }


}


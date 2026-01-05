package com.collederas.kroll.security.apikey

import com.collederas.kroll.support.MutableTestClock
import com.collederas.kroll.support.TestClockConfig
import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig::class)
@Transactional
@ActiveProfiles("test")
class ApiKeyIntegrationTests {

    @Autowired
    lateinit var envFactory: PersistedEnvironmentFactory

    @Autowired
    lateinit var apiKeyService: ApiKeyService

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var clock: MutableTestClock

    @Test
    fun `deleted api key is immediately invalidated`() {
        val env = envFactory.create()
        val created = apiKeyService.create(env.id, Instant.now().plus(Duration.ofDays(1)))

        mvc.post("/client/config/fetch") {
            header("X-Api-Key", created.key)
        }.andExpect {
            status { isOk() }
        }

        apiKeyService.delete(created.id)

        mvc.get("/client/config/fetch") {
            header("X-Api-Key", created.key)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `expired api key does not authenticate`() {
        val env = envFactory.create()

        val key = apiKeyService.create(
            env.id,
            clock.instant().plusSeconds(10)
        )

        clock.advanceBy(Duration.ofSeconds(11))

        mvc.post("/client/config/fetch") {
            header("X-Api-Key", key.key)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `unknown api key does not authenticate`() {
        mvc.post("/client/config/fetch") {
            header("X-Api-Key", "rk_definitely_not_existing")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}

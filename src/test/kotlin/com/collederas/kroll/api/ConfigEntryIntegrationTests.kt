package com.collederas.kroll.api

import com.collederas.kroll.core.config.CreateConfigEntryDto
import com.collederas.kroll.core.config.UpdateConfigEntryDto
import com.collederas.kroll.core.config.entry.ConfigEntryService
import com.collederas.kroll.core.config.entry.ConfigType
import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.support.MutableTestClock
import com.collederas.kroll.support.TestClockConfig
import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import com.collederas.kroll.support.factories.UserFactory
import com.collederas.kroll.user.AppUserRepository
import com.collederas.kroll.user.UserRole
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Duration

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig::class)
@ActiveProfiles("test")
class ConfigEntryIntegrationTests {
    @Autowired
    private lateinit var userRepository: AppUserRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var envFactory: PersistedEnvironmentFactory

    @Autowired
    private lateinit var configEntryService: ConfigEntryService

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var clock: MutableTestClock

    private val testedEndpoint = "/environments/{envId}/configs"

    @ParameterizedTest(name = "{0} to {1} should be Forbidden")
    @CsvSource(
        "GET,       /environments/{envId}/configs",
        "PUT,       /environments/{envId}/configs/{configKey}",
        "DELETE,    /environments/{envId}/configs/{configKey}",
    )
    fun `admins cannot access unowned config entries`(
        method: String,
        urlTemplate: String,
    ) {
        val unauthorizedUser = UserFactory.create(roles = setOf(UserRole.ADMIN))
        val victimUser = UserFactory.create(roles = setOf(UserRole.ADMIN))

        userRepository.save(victimUser)
        val victimEnv = envFactory.create(victimUser)
        val targetEntry =
            configEntryService.create(
                victimUser.id,
                victimEnv.id,
                CreateConfigEntryDto(
                    type = ConfigType.STRING,
                    key = "victimKey",
                    value = "original",
                ),
            )

        val requestBody =
            UpdateConfigEntryDto(
                value = "pwned",
            )

        val unauthorizedAuthUser = AuthUserDetails(unauthorizedUser)
        val request =
            when (method) {
                "PUT" ->
                    mvc.put(urlTemplate, victimEnv.id, targetEntry.key) {
                        with(SecurityMockMvcRequestPostProcessors.user(unauthorizedAuthUser))
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(requestBody)
                    }

                "DELETE" ->
                    mvc.delete(urlTemplate, victimEnv.id, targetEntry.key) {
                        with(SecurityMockMvcRequestPostProcessors.user(unauthorizedAuthUser))
                    }

                else ->
                    mvc.get(urlTemplate, victimEnv.id) {
                        with(SecurityMockMvcRequestPostProcessors.user(unauthorizedAuthUser))
                    }
            }

        request.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `create should persist and return created config entry`() {
        val user = userRepository.save(UserFactory.create(roles = setOf(UserRole.ADMIN)))
        val env = envFactory.create(user)

        val activeFrom = clock.instant()
        val activeUntil = activeFrom.plus(Duration.ofHours(1))

        val createRequest =
            mapOf(
                "key" to "key.id",
                "value" to "true",
                "type" to "BOOLEAN",
                "activeFrom" to activeFrom.toString(),
                "activeUntil" to activeUntil.toString(),
            )

        val authUser = AuthUserDetails(user)

        mvc
            .post(testedEndpoint, env.id) {
                with(SecurityMockMvcRequestPostProcessors.user(authUser))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(createRequest)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.key") { value("key.id") }
                jsonPath("$.value") { value("true") }
                jsonPath("$.type") { value("BOOLEAN") }
                jsonPath("$.activeFrom") { value(activeFrom.toString()) }
                jsonPath("$.activeUntil") { value(activeUntil.toString()) }
            }

        val persisted = configEntryService.list(authUser.getId(), env.id).single()
        Assertions.assertThat(persisted.key).isEqualTo("key.id")
        Assertions.assertThat(persisted.value).isEqualTo("true")
        Assertions.assertThat(persisted.type).isEqualTo(ConfigType.BOOLEAN)
        Assertions.assertThat(persisted.activeFrom).isEqualTo(activeFrom)
        Assertions.assertThat(persisted.activeUntil).isEqualTo(activeUntil)
    }

    @Test
    fun `update should persist and return updated config entry`() {
        val user = userRepository.save(UserFactory.create(roles = setOf(UserRole.ADMIN)))
        val env = envFactory.create(user)

        configEntryService.create(
            user.id,
            env.id,
            CreateConfigEntryDto(
                key = "key.id",
                value = "old_value",
                type = ConfigType.STRING,
            ),
        )

        val updateRequest =
            mapOf(
                "value" to "new_value",
                "type" to "STRING",
            )

        val authUser = AuthUserDetails(user)

        mvc
            .put("$testedEndpoint/{key}", env.id, "key.id") {
                with(SecurityMockMvcRequestPostProcessors.user(authUser))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(updateRequest)
            }.andExpect {
                status { isOk() }
                jsonPath("$.key") { value("key.id") }
                jsonPath("$.value") { value("new_value") }
                jsonPath("$.type") { value("STRING") }
            }

        val persisted = configEntryService.list(authUser.getId(), env.id).single()
        Assertions.assertThat(persisted.value).isEqualTo("new_value")
        Assertions.assertThat(persisted.type).isEqualTo(ConfigType.STRING)
    }

    @Test
    fun `update supports dotted config keys`() {
        val user = userRepository.save(UserFactory.create(roles = setOf(UserRole.ADMIN)))
        val env = envFactory.create(user)

        configEntryService.create(
            user.id,
            env.id,
            CreateConfigEntryDto(
                key = "db.password.prod",
                value = "old",
                type = ConfigType.STRING,
            ),
        )

        val updateRequest =
            UpdateConfigEntryDto(
                value = "new",
            )

        val authUser = AuthUserDetails(user)

        mvc
            .put("$testedEndpoint/{key}", env.id, "db.password.prod") {
                with(SecurityMockMvcRequestPostProcessors.user(authUser))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(updateRequest)
            }.andExpect {
                status { isOk() }
                jsonPath("$.key") { value("db.password.prod") }
                jsonPath("$.value") { value("new") }
            }

        val persisted = configEntryService.list(authUser.getId(), env.id).single()
        Assertions.assertThat(persisted.key).isEqualTo("db.password.prod")
        Assertions.assertThat(persisted.value).isEqualTo("new")
    }
}

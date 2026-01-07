package com.collederas.kroll.remoteconfig.configentry

import com.collederas.kroll.core.configentry.ConfigEntryService
import com.collederas.kroll.core.configentry.ConfigType
import com.collederas.kroll.core.configentry.dto.CreateConfigEntryDto
import com.collederas.kroll.core.configentry.dto.UpdateConfigEntryDto
import com.collederas.kroll.security.identity.AuthUserDetails
import com.collederas.kroll.support.factories.PersistedEnvironmentFactory
import com.collederas.kroll.support.factories.UserFactory
import com.collederas.kroll.user.AppUserRepository
import com.collederas.kroll.user.UserRole
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.*
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test", "test-persistence")
class ConfigEntryIntegrationTests() {
    @Autowired
    private lateinit var userRepository: AppUserRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var envFactory: PersistedEnvironmentFactory

    @Autowired
    lateinit var configEntryService: ConfigEntryService

    @Autowired
    lateinit var mvc: MockMvc

    private val testedEndpoint = "/admin/environments/{envId}/configs"

    @ParameterizedTest(name = "{0} to {1} should be Forbidden")
    @CsvSource(
        "GET,       /admin/environments/{envId}/configs",
        "PUT,       /admin/environments/{envId}/configs/{configKey}",
        "DELETE,    /admin/environments/{envId}/configs/{configKey}"
    )
    fun `users cannot access unowned config entries`(method: String, urlTemplate: String) {
        val unauthorizedUser = UserFactory.create(roles = setOf(UserRole.ADMIN))
        val victimUser = UserFactory.create(roles = setOf(UserRole.ADMIN))

        userRepository.save(victimUser)
        val victimEnv = envFactory.create(victimUser)
        val targetEntry = configEntryService.create(
            victimUser.id, victimEnv.id, CreateConfigEntryDto(
                type = ConfigType.STRING,
                key = "victimKey",
                value = "original"
            )
        )

        val requestBody = UpdateConfigEntryDto(
            value = "pwned"
        )

        val unauthorizedAuthUser = AuthUserDetails(unauthorizedUser)
        val request = when (method) {
            "PUT" -> mvc.put(urlTemplate, victimEnv.id, targetEntry.key) {
                with(user(unauthorizedAuthUser))
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(requestBody)
            }

            "DELETE" -> mvc.delete(urlTemplate, victimEnv.id, targetEntry.key) {
                with(user(unauthorizedAuthUser))
            }

            else -> mvc.get(urlTemplate, victimEnv.id) {
                with(user(unauthorizedAuthUser))
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

        val activeFrom = Instant.now()
        val activeUntil = activeFrom.plus(Duration.ofHours(1))

        val createRequest = mapOf(
            "key" to "key.id",
            "value" to "true",
            "type" to "BOOLEAN",
            "activeFrom" to activeFrom.toString(),
            "activeUntil" to activeUntil.toString()
        )

        val authUser = AuthUserDetails(user)

        mvc.post(testedEndpoint, env.id) {
            with(user(authUser))
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
        assertThat(persisted.key).isEqualTo("key.id")
        assertThat(persisted.value).isEqualTo("true")
        assertThat(persisted.type).isEqualTo(ConfigType.BOOLEAN)
        assertThat(persisted.activeFrom).isEqualTo(activeFrom)
        assertThat(persisted.activeUntil).isEqualTo(activeUntil)
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
            )
        )

        val updateRequest = mapOf(
            "value" to "new_value",
            "type" to "STRING"
        )

        val authUser = AuthUserDetails(user)

        mvc.put("$testedEndpoint/{key}", env.id, "key.id") {
            with(user(authUser))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updateRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.key") { value("key.id") }
            jsonPath("$.value") { value("new_value") }
            jsonPath("$.type") { value("STRING") }
        }

        val persisted = configEntryService.list(authUser.getId(), env.id).single()
        assertThat(persisted.value).isEqualTo("new_value")
        assertThat(persisted.type).isEqualTo(ConfigType.STRING)
    }
}

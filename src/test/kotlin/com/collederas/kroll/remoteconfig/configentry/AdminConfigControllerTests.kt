package com.collederas.kroll.remoteconfig.configentry

import com.collederas.kroll.core.configentry.ConfigEntryService
import com.collederas.kroll.core.configentry.ConfigType
import com.collederas.kroll.core.configentry.dto.ConfigEntryResponseDto
import com.collederas.kroll.security.user.AuthUserDetails
import com.collederas.kroll.support.factories.EnvironmentFactory
import com.collederas.kroll.support.factories.UserFactory
import com.collederas.kroll.user.UserRole
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Duration
import java.time.Instant
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminConfigControllerTests(
    @Autowired private val objectMapper: ObjectMapper
) {
    @MockkBean
    lateinit var configEntryService: ConfigEntryService

    @Autowired
    lateinit var mvc: MockMvc

    // Shared Test Data
    private lateinit var envId: UUID
    private lateinit var authUser: AuthUserDetails
    private val configKey = "feature.flag.enabled"

    @BeforeEach
    fun setup() {
        val env = EnvironmentFactory.create()
        envId = env.id

        val user = UserFactory.create(roles = setOf(UserRole.ADMIN))
        authUser = AuthUserDetails(user = user)
    }

    @Test
    fun `create should return created config entry`() {
        val activeFrom = Instant.now()
        val activeUntil = activeFrom.plus(Duration.ofHours(1))

        val expectedResponseDto = ConfigEntryResponseDto(
            environmentId = envId,
            key = configKey,
            type = ConfigType.BOOLEAN,
            value = "true",
            activeFrom = activeFrom,
            activeUntil = activeUntil
        )

        // Capture arguments to verify the controller mapping
        every {
            configEntryService.create(any(), any(), any())
        } returns expectedResponseDto

        val createRequest = mapOf(
            "key" to configKey,
            "value" to "true",
            "type" to "BOOLEAN",
            "activeFrom" to activeFrom.toString(),
            "activeUntil" to activeUntil.toString()
        )

        mvc.perform(
            post("/admin/environments/$envId/configs")
                .with(user(authUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.key").value(configKey))
            .andExpect(jsonPath("$.value").value("true"))
            .andExpect(jsonPath("$.type").value("BOOLEAN"))
            .andExpect(jsonPath("$.activeFrom").value(activeFrom.toString()))

        verify(exactly = 1) {
            configEntryService.create(
                eq(envId),
                any(),
                any()
            )
        }
    }

    @Test
    fun `update should return updated config entry`() {
        val activeFrom = Instant.now()
        val activeUntil = activeFrom.plus(Duration.ofHours(24))

        val expectedResponseDto = ConfigEntryResponseDto(
            environmentId = envId,
            key = configKey,
            type = ConfigType.STRING,
            value = "new_value",
            activeFrom = activeFrom,
            activeUntil = activeUntil
        )

        every {
            configEntryService.update(any(), any(), any(), any())
        } returns expectedResponseDto

        val updateRequest = mapOf(
            "value" to "new_value",
            "type" to "STRING"
        )

        mvc.perform(
            put("/admin/environments/$envId/configs/$configKey")
                .with(user(authUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.key").value(configKey))
            .andExpect(jsonPath("$.value").value("new_value"))
            .andExpect(jsonPath("$.type").value("STRING"))
            .andExpect(jsonPath("$.activeUntil").value(activeUntil.toString()))

        verify(exactly = 1) {
            configEntryService.update(
                eq(authUser.getId()),
                eq(envId),
                eq(configKey),
                any()
            )
        }
    }
}

package com.collederas.kroll.security.apikey

import com.collederas.kroll.core.environment.EnvironmentRepository
import com.collederas.kroll.core.exceptions.EnvironmentNotFoundException
import com.collederas.kroll.security.apikey.exception.InvalidApiKeyExpiryException
import com.collederas.kroll.support.factories.EnvironmentFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.data.repository.findByIdOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class ApiKeyServiceTests {
    private val environmentRepo = mockk<EnvironmentRepository>()
    private val apiRepo = mockk<ApiKeyRepository>()
    private val defaultConfig = ApiKeyConfigProperties()

    private val fixedNow = Instant.parse("2025-01-01T00:00:00Z")
    private val fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private val apiKeyService =
        ApiKeyService(
            apiRepo,
            environmentRepo,
            defaultConfig,
            fixedClock,
        )

    @Test
    fun `create throws when environment is missing`() {
        val envId = UUID.randomUUID()
        every { environmentRepo.findByIdOrNull(envId) } returns null

        assertThrows(EnvironmentNotFoundException::class.java) {
            apiKeyService.create(envId, fixedNow.plus(Duration.ofDays(1)))
        }
    }

    @Test
    fun `create throws when expiry is exactly now`() {
        val envId = UUID.randomUUID()
        val environment = EnvironmentFactory.create()

        every { environmentRepo.findByIdOrNull(envId) } returns environment

        assertThrows(InvalidApiKeyExpiryException::class.java) {
            apiKeyService.create(envId, fixedNow)
        }
    }

    @Test
    fun `create accepts expiry exactly at max lifetime`() {
        val max = defaultConfig.maxLifetime
        val expiresAt = fixedNow.plus(max)

        every { environmentRepo.findByIdOrNull(any()) } returns EnvironmentFactory.create()
        every { apiRepo.save(any()) } answers { firstArg() }

        apiKeyService.create(UUID.randomUUID(), expiresAt)
    }

    @Test
    fun `create throws expiry is beyond max lifetime`() {
        val environment = EnvironmentFactory.create()
        val max = defaultConfig.maxLifetime
        val expiresAt = fixedNow.plus(max).plus(Duration.ofMillis(1))

        every { environmentRepo.findByIdOrNull(any()) } returns environment
        assertThrows(InvalidApiKeyExpiryException::class.java) {
            apiKeyService.create(environment.id, expiresAt)
        }
    }

    @Test
    fun `create key successfully`() {
        val envId = UUID.randomUUID()
        val environment = EnvironmentFactory.create()
        val expiresAt = fixedNow.plus(Duration.ofDays(1))
        every { environmentRepo.findByIdOrNull(envId) } returns environment

        val keySlot = slot<ApiKeyEntity>()
        every { apiRepo.save(capture(keySlot)) } answers { keySlot.captured }

        val response = apiKeyService.create(envId, expiresAt)

        assertNotNull(response.id)
        assertNotNull(response.key)
        assertEquals(response.expiresAt, expiresAt)

        val persisted = keySlot.captured
        assertNotEquals(response.key, persisted.keyHash)
        assertEquals(ApiKeyHasher.hash(response.key), persisted.keyHash)

        assertEquals(expiresAt, persisted.expiresAt)
        assertEquals(environment, persisted.environment)
        assertFalse(persisted.mask.contains(response.key))
        assertTrue(persisted.mask.contains("..."))
        assertEquals(7 + 3 + 4, persisted.mask.length)
    }

    @Test
    fun `create generates different keys every time`() {
        val environment = EnvironmentFactory.create()
        every { environmentRepo.findByIdOrNull(any()) } returns environment
        every { apiRepo.save(any()) } answers { firstArg() }

        val expiresAt = fixedNow.plus(Duration.ofDays(1))

        val r1 = apiKeyService.create(environment.id, expiresAt)
        val r2 = apiKeyService.create(environment.id, expiresAt)

        assertNotEquals(r1.key, r2.key)
    }
}

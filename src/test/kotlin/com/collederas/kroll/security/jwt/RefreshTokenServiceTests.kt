package com.collederas.kroll.security.jwt

import com.collederas.kroll.support.factories.UserFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.*

class RefreshTokenServiceTests {
    private lateinit var refreshTokenService: RefreshTokenService
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var properties: RefreshTokenProperties

    @BeforeEach
    fun setup() {
        refreshTokenRepository = mockk()
        properties = RefreshTokenProperties(Duration.ofDays(1))
        refreshTokenService =
            RefreshTokenService(
                repository = refreshTokenRepository,
                properties = properties,
            )
    }

    @Test
    fun `issueTokenFor persists the right owner`() {
        val user = UserFactory.create()
        val slot = slot<RefreshTokenEntity>()

        every { refreshTokenRepository.save(capture(slot)) } answers { slot.captured }

        val returnedToken = refreshTokenService.issueTokenFor(user)
        val savedEntity = slot.captured

        assertEquals(user, savedEntity.owner)
        assertEquals(returnedToken, savedEntity.token)
        assertTrue(savedEntity.expiresAt.isAfter(Instant.now()))
    }

    @Test
    fun `issueTokenFor returns a correctly formatted token`() {
        val user = UserFactory.create()

        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        val returnedToken = refreshTokenService.issueTokenFor(user)

        val decoded = Base64.getUrlDecoder().decode(returnedToken)
        assertEquals(32, decoded.size)
        assertTrue(returnedToken.matches(Regex("^[A-Za-z0-9_-]+$")))
    }

    @Test
    fun `rotateAllTokensFor deletes old tokens and creates new one`() {
        val user = UserFactory.create()
        every { refreshTokenRepository.deleteAllByOwner(user) } returns Unit

        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        val newToken = refreshTokenService.rotateAllTokensFor(user)

        verify { refreshTokenRepository.deleteAllByOwner(user) }
        verify { refreshTokenRepository.save(any()) }
        assertNotNull(newToken)
    }

    @Test
    fun `consumeToken returns user and deletes token`() {
        val user = UserFactory.create()
        val entity =
            RefreshTokenEntity(
                owner = user,
                token = "abc",
                expiresAt = Instant.now().plusSeconds(60),
            )

        every { refreshTokenRepository.findByToken("abc") } returns entity
        every { refreshTokenRepository.delete(entity) } returns Unit

        val returnedUser = refreshTokenService.consumeToken("abc")

        assert(returnedUser == user)
        verify { refreshTokenRepository.delete(entity) }
    }

    @Test
    fun `consumeToken throws if expired`() {
        val user = UserFactory.create()
        val entity =
            RefreshTokenEntity(
                owner = user,
                token = "expired",
                expiresAt = Instant.now().minusSeconds(10),
            )

        every { refreshTokenRepository.findByToken("expired") } returns entity
        every { refreshTokenRepository.delete(entity) } returns Unit

        assertThrows(IllegalArgumentException::class.java) {
            refreshTokenService.consumeToken("expired")
        }
    }

    @Test
    fun `rotateFromRefresh consumes old and returns new`() {
        val user = UserFactory.create()
        val old =
            RefreshTokenEntity(
                owner = user,
                token = "old",
                expiresAt = Instant.now().plusSeconds(60),
            )

        every { refreshTokenRepository.findByToken("old") } returns old
        every { refreshTokenRepository.delete(old) } returns Unit
        every { refreshTokenRepository.save(any()) } answers { firstArg<RefreshTokenEntity>() }

        val (returnedUser, newToken) = refreshTokenService.rotateFromRefresh("old")

        assert(returnedUser == user)
        assert(newToken != "old")
        verify { refreshTokenRepository.delete(old) }
        verify { refreshTokenRepository.save(any()) }
    }
}

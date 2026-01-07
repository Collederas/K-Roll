package com.collederas.kroll.security.jwt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.*

@ActiveProfiles("test")
class JwtServiceTests {
    private val secret = "bzjKq1tb2/7FmHzg51eH8ZPqfm4twB4suaT2H8CYm8A="

    private fun newService(exp: Duration = Duration.ofHours(1)): JwtTokenService {
        val props =
            JwtProperties(
                secret = secret,
                expiration = exp,
            )
        return JwtTokenService(props)
    }

    @Test
    fun `generate then validate should return same user id`() {
        val userId: UUID = UUID.randomUUID()
        val jwtService = newService()
        val token = jwtService.generateToken(userId, "testUser")

        val extracted: UUID? = jwtService.validateAndGetUserId(token)

        assertThat(extracted).isEqualTo(userId)
    }

    @Test
    fun `expired token must fail validation`() {
        val userId: UUID = UUID.randomUUID()
        val jwtService = newService(exp = Duration.ofMillis(-1))

        val token = jwtService.validateAndGetUserId(jwtService.generateToken(userId, "testUser"))

        assertThat(token).isNull()
    }

    @Test
    fun `invalid signature must fail validation`() {
        val userId: UUID = UUID.randomUUID()
        val krollJwtService = newService()
        val invalidProps =
            JwtProperties(
                secret = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                expiration = Duration.ofHours(1),
            )

        val maliciousService = JwtTokenService(invalidProps)
        val maliciousToken = maliciousService.generateToken(userId, "testUser")
        val result = krollJwtService.validateAndGetUserId(maliciousToken)

        assertThat(result).isNull()
    }
}

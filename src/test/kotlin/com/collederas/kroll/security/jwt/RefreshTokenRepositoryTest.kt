package com.collederas.kroll.security.jwt

import com.collederas.kroll.support.factories.UserFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant

@DataJpaTest
class RefreshTokenRepositoryTest {
    @Autowired
    lateinit var repository: RefreshTokenRepository

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Test
    fun `findByToken returns null when token does not exist`() {
        val token = "nonexistent-token"
        val result = repository.findByToken(token)
        assertThat(result).isNull()
    }

    @Test
    fun `findByToken returns token when it exists`() {
        val user = UserFactory.create()
        entityManager.persist(user)

        val token = RefreshTokenEntity(owner = user, token = "token", expiresAt = Instant.now())
        entityManager.persist(token)
        entityManager.flush()

        val result = repository.findByToken("token")
        assertThat(result).isNotNull()
        assertThat(result?.token).isEqualTo("token")
    }

    @Test
    fun `deleteAllByOwner removes all tokens for specific user`() {
        val user = UserFactory.create()
        entityManager.persist(user)

        val token1 = RefreshTokenEntity(owner = user, token = "token1", expiresAt = Instant.now())
        val token2 = RefreshTokenEntity(owner = user, token = "token2", expiresAt = Instant.now())
        entityManager.persist(token1)
        entityManager.persist(token2)
        entityManager.flush()

        repository.deleteAllByOwner(user)

        assertThat(repository.findAll()).isEmpty()
    }

    @Test
    fun `throws DataIntegrityViolationException when saving duplicate token`() {
        val user = UserFactory.create()
        entityManager.persist(user)

        val token1 = RefreshTokenEntity(owner = user, token = "same-token", expiresAt = Instant.now())
        repository.save(token1)

        val token2 = RefreshTokenEntity(owner = user, token = "same-token", expiresAt = Instant.now())

        assertThrows(DataIntegrityViolationException::class.java) {
            repository.saveAndFlush(token2)
        }
    }
}

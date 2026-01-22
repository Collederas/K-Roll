package com.collederas.kroll.bootstrap.data

import com.collederas.kroll.user.AppUser
import com.collederas.kroll.user.AppUserRepository
import com.collederas.kroll.user.UserRole
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.*

@Component
@Profile("dev")
class DevDataSeeder(
    private val appUserRepository: AppUserRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        if (appUserRepository.count() == 0L) {
            seedTestUser()
        }
    }

    private fun seedTestUser() {
        println("🌱 Seeding initial test user...")

        val testEmail = "test.user@example.com"
        val testUsername = "test"
        val rawPassword = "password123"

        val hashedPassword = passwordEncoder.encode(rawPassword)

        val testUser =
            AppUser(
                id = UUID.randomUUID(),
                email = testEmail,
                username = testUsername,
                passwordHash = hashedPassword,
                roles = setOf(UserRole.ADMIN),
            )

        appUserRepository.save(testUser)

        println("✅ Successfully seeded test user: ${testUser.username} (Password: $rawPassword)")
    }
}

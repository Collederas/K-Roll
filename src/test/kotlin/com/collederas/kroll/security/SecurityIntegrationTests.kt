package com.collederas.kroll.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTests {
    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun `request to non-existent endpoint should return 404`() {
        mvc.get("/non-existent-endpoint-${java.util.UUID.randomUUID()}")
            .andExpect {
                status { isNotFound() }
            }
    }
}

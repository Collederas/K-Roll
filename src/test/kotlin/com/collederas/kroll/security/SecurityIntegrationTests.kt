package com.collederas.kroll.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
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
    @WithMockUser(roles = ["ADMIN"])
    fun `authd request to non-existent endpoint should return 404`() {
        mvc
            .get("/non-existent-endpoint-${java.util.UUID.randomUUID()}")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `non-authd request to non-existent endpoint should return 401`() {
        mvc
            .get("/non-existent-endpoint-${java.util.UUID.randomUUID()}")
            .andExpect {
                status { isUnauthorized() }
            }
    }
}

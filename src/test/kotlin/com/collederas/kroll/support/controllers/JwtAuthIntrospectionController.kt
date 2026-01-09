package com.collederas.kroll.support.controllers

import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test/auth/jwt/")
class JwtAuthIntrospectionController {
    @GetMapping("/whoami")
    fun whoAmI(authentication: Authentication?): Any {
        if (authentication == null) {
            return mapOf("authenticated" to false)
        }
        return mapOf(
            "authenticated" to authentication.isAuthenticated,
            "principal" to authentication.principal,
            "authorities" to authentication.authorities.map { it.authority },
        )
    }
}

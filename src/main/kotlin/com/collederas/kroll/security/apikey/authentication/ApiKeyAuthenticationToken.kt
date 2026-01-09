package com.collederas.kroll.security.apikey.authentication

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class ApiKeyAuthenticationToken : AbstractAuthenticationToken {
    private val principalValue: Any?
    private val credentialsValue: Any?

    // Unauthenticated
    constructor(rawApiKey: String) : super(null) {
        this.principalValue = null
        this.credentialsValue = rawApiKey
        isAuthenticated = false
    }

    // Authenticated
    constructor(
        principal: Any,
        credentials: Any?,
        authorities: Collection<GrantedAuthority>,
    ) : super(authorities) {
        this.principalValue = principal
        this.credentialsValue = credentials
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = credentialsValue

    override fun getPrincipal(): Any? = principalValue
}

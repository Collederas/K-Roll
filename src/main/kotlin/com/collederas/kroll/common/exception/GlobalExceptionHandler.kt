package com.collederas.kroll.common.exception

import com.collederas.kroll.remoteconfig.exceptions.ProjectAlreadyExistsException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(e: AuthenticationException): ResponseEntity<Map<String, String?>> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to (e.message ?: "Authentication failed")))
    }

    @ExceptionHandler(ProjectAlreadyExistsException::class)
    fun handleDuplicateProject(ex: ProjectAlreadyExistsException): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(mapOf("error" to (ex.message ?: "Project already exists")))
    }
}

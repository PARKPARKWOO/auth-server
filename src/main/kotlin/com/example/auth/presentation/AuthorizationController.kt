package com.example.auth.presentation

import com.example.auth.business.RegistrationService
import com.example.auth.presentation.request.RegistrationUserRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthorizationController(
    private val registrationService: RegistrationService,
) {
    @PostMapping("/registration")
    suspend fun register(
        @RequestBody
        request: RegistrationUserRequest,
    ) {
        registrationService.registerUser(request.toCommand())
    }
}

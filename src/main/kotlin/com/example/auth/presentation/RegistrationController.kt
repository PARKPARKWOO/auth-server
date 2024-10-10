package com.example.auth.presentation

import com.example.auth.business.service.RegistrationService
import com.example.auth.presentation.request.RegistrationApplicationOAuthRequest
import com.example.auth.presentation.request.RegistrationApplicationRequest
import com.example.auth.presentation.request.RegistrationUserRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/registration")
class RegistrationController(
    private val registrationService: RegistrationService,
) {
    @PostMapping("/user")
    suspend fun registerUser(
        @RequestBody
        request: RegistrationUserRequest,
    ): String {
        return registrationService.registerUser(request.toCommand())
    }

    @PostMapping("/application")
    suspend fun registerApplication(
        @RequestBody
        request: RegistrationApplicationRequest,
    ): String {
        return registrationService.registerApplication(request.name)
    }

    @PostMapping("/application/oauth")
    suspend fun registerApplicationOAuth(
        @RequestBody
        request: RegistrationApplicationOAuthRequest,
    ): Long {
        return registrationService.registerApplicationOAuthProvider(request.toCommand())
    }
}

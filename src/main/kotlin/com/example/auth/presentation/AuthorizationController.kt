package com.example.auth.presentation

import com.example.auth.business.RegistrationService
import kotlinx.coroutines.reactor.mono
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthorizationController(
    private val registrationService: RegistrationService,
) {
    @PostMapping("/sign-up")
    fun register() = mono {

    }
}
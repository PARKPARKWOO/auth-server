package com.example.auth.presentation

import com.example.auth.business.service.ApplicationOAuthService
import com.example.auth.business.service.RegistrationService
import com.example.auth.common.http.response.SucceededApiResponseBody
import com.example.auth.presentation.request.RegistrationApplicationOAuthRequest
import com.example.auth.presentation.request.RegistrationApplicationRequest
import com.example.auth.presentation.request.RegistrationDomainRequest
import com.example.auth.presentation.request.RegistrationUserRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/registration")
class RegistrationController(
    private val registrationService: RegistrationService,
    private val applicationOAuthService: ApplicationOAuthService,
) {
    @PostMapping("/user")
    suspend fun registerUser(
        @RequestBody
        request: RegistrationUserRequest,
    ): SucceededApiResponseBody<String> {
        val response = registrationService.registerUser(request.toCommand()).id
        return SucceededApiResponseBody(response)
    }

    @PostMapping("/application")
    suspend fun registerApplication(
        @RequestBody
        request: RegistrationApplicationRequest,
    ): SucceededApiResponseBody<String> {
        val response = registrationService.registerApplication(
            name = request.name,
            redirectUrl = request.redirectUrl,
        )
        return SucceededApiResponseBody(response)
    }

    @PostMapping("/application/oauth")
    suspend fun registerApplicationOAuth(
        @RequestBody
        request: RegistrationApplicationOAuthRequest,
    ): SucceededApiResponseBody<Long> {
        val response = applicationOAuthService.register(request.toCommand())
        return SucceededApiResponseBody(response)
    }

    @PostMapping("/application/domain")
    suspend fun registerDomain(
        @RequestBody
        request: RegistrationDomainRequest,
    ): SucceededApiResponseBody<Unit> {
        registrationService.registerApplicationDomainsForCors(request.toCommand())
        return SucceededApiResponseBody.unit()
    }
}

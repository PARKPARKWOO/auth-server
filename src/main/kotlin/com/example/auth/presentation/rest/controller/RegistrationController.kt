package com.example.auth.presentation.rest.controller

import com.example.auth.business.service.application.ApplicationOAuthService
import com.example.auth.business.service.RegistrationService
import com.example.auth.presentation.rest.controller.request.RegistrationApplicationOAuthRequest
import com.example.auth.presentation.rest.controller.request.RegistrationApplicationRequest
import com.example.auth.presentation.rest.controller.request.RegistrationDomainRequest
import com.example.auth.presentation.rest.controller.request.RegistrationUserRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.woo.http.SucceededApiResponseBody

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
        val response =
            registrationService.registerApplication(
                name = request.name,
                redirectUrl = request.redirectUrl,
                redirectType = request.redirectType,
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
        return SucceededApiResponseBody(data = null)
    }
}

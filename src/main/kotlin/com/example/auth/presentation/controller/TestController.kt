package com.example.auth.presentation.controller

import com.example.auth.business.service.ApplicationOAuthService
import com.example.auth.business.service.dto.ClientRegistrationInfoDto
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class TestController(
    private val applicationOAuthService: ApplicationOAuthService,
) {
    @GetMapping("/test2")
    suspend fun getDto(request: ServerHttpRequest): List<ClientRegistrationInfoDto> {
        return applicationOAuthService.findClientRegistrationInfoDto()
    }
}

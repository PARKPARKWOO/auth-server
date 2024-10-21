package com.example.auth.presentation

import com.example.auth.business.service.ApplicationOAuthService
import com.example.auth.business.service.dto.ClientRegistrationInfoDto
import com.example.auth.domain.model.oauth.SocialProvider
import com.example.auth.domain.repository.DynamicReactiveClientRegistrationRepositoryImpl.Companion.CLIENT_REGISTRATION_KEY
import com.example.auth.domain.repository.redis.RedisDriver
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class TestController(
    private val redisDriver: RedisDriver,
    private val applicationOAuthService: ApplicationOAuthService,
) {
    @GetMapping("/test")
    suspend fun oauth() {
        val findAll = redisDriver.setFindAll(CLIENT_REGISTRATION_KEY, ClientRegistration::class.java)
        val old = findAll.find { it.registrationId == "14" }
        val dd = CommonOAuth2Provider.GOOGLE
            .getBuilder("ddddd")
            .redirectUri("Ddd")
            .clientId("dddd")
            .clientSecret("ddd")
            .clientName(SocialProvider.GOOGLE.clientNamePrefix)
            .build()
        redisDriver.updateSetForSingle(CLIENT_REGISTRATION_KEY, old, dd)
    }

    @GetMapping("/test2")
    suspend fun getDto(): List<ClientRegistrationInfoDto> {
        return applicationOAuthService.findClientRegistrationInfoDto()
    }
}

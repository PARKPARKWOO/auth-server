package com.example.auth.presentation

import com.example.auth.domain.repository.DynamicReactiveClientRegistrationRepositoryImpl
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class TestController(
    private val repo: DynamicReactiveClientRegistrationRepositoryImpl,
) {
    @GetMapping("/test/{id}")
    suspend fun oauth(@PathVariable id: Long): ClientRegistration? {
        return repo.findByRegistrationId(id.toString()).awaitSingle()
    }
}

package com.example.auth.domain.repository.application

import com.example.auth.domain.entity.application.ApplicationOAuthProvider
import com.example.auth.domain.model.oauth.SocialProvider
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface ApplicationOAuthProviderRepository : ReactiveCrudRepository<ApplicationOAuthProvider, Long> {

    fun findByApplicationIdAndProvider(applicationId: String, provider: SocialProvider): Mono<ApplicationOAuthProvider>
}

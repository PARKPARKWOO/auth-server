package com.example.auth.domain.repository

import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import reactor.core.publisher.Mono
import java.util.*

interface DynamicReactiveClientRegistrationRepository : ReactiveClientRegistrationRepository {
    fun addRegistration(registration: ClientRegistration): Mono<Void>

    fun updateRegistration(registration: ClientRegistration): Mono<Void>

    fun removeRegistration(registrationId: String): Mono<Void>
}

class DynamicReactiveClientRegistrationRepositoryImpl(
    private val initialRegistrations: List<ClientRegistration>,
) : DynamicReactiveClientRegistrationRepository {
    private var registrations: MutableList<ClientRegistration> = Collections.synchronizedList(initialRegistrations)

    override fun addRegistration(registration: ClientRegistration): Mono<Void> {
        TODO("Not yet implemented")
    }

    override fun updateRegistration(registration: ClientRegistration): Mono<Void> {
        TODO("Not yet implemented")
    }

    override fun removeRegistration(registrationId: String): Mono<Void> {
        TODO("Not yet implemented")
    }

    override fun findByRegistrationId(registrationId: String?): Mono<ClientRegistration> {
        return Mono.justOrEmpty(registrations.find { it.registrationId == registrationId })
    }
}

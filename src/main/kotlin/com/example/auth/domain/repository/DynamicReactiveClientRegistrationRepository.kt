package com.example.auth.domain.repository

import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

interface DynamicReactiveClientRegistrationRepository : ReactiveClientRegistrationRepository {
    suspend fun addRegistration(registration: ClientRegistration)

    suspend fun updateRegistration(registration: ClientRegistration)

    suspend fun removeRegistration(registrationId: String)
}

@Component
class DynamicReactiveClientRegistrationRepositoryImpl : DynamicReactiveClientRegistrationRepository {
    private val registrations: ConcurrentMap<String, ClientRegistration> = ConcurrentHashMap()

    fun initialize(initialRegistrations: List<ClientRegistration>) {
        registrations.putAll(initialRegistrations.associateBy { it.registrationId })
    }

    override suspend fun addRegistration(registration: ClientRegistration) {
        registrations[registration.registrationId] = registration
    }

    override suspend fun updateRegistration(registration: ClientRegistration) {
        registrations[registration.registrationId] = registration
    }

    override suspend fun removeRegistration(registrationId: String) {
        registrations.remove(registrationId)
    }

    override fun findByRegistrationId(registrationId: String?): Mono<ClientRegistration> {
        return Mono.justOrEmpty(registrations[registrationId])
    }
}

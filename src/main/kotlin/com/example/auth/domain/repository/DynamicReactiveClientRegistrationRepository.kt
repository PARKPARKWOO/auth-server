package com.example.auth.domain.repository

import com.example.auth.domain.repository.redis.RedisDriver
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

@Component("DynamicReactiveClientRegistrationRepositoryImpl")
class DynamicReactiveClientRegistrationRepositoryImpl(
    private val redisDriver: RedisDriver,
) : DynamicReactiveClientRegistrationRepository {
    private val registrations: ConcurrentMap<String, ClientRegistration> = ConcurrentHashMap()

    companion object {
        const val CLIENT_REGISTRATION_KEY = "client:registration"
    }

    suspend fun initialize(initialRegistrations: List<ClientRegistration>) {
        registrations.putAll(initialRegistrations.associateBy { it.registrationId })
//        redisDriver.addSetForMultiValue(CLIENT_REGISTRATION_KEY, initialRegistrations.toTypedArray())
    }

    override suspend fun addRegistration(registration: ClientRegistration) {
        registrations.put(registration.registrationId, registration)
        redisDriver.addSetForSingle(CLIENT_REGISTRATION_KEY, registration)
    }

    override suspend fun updateRegistration(registration: ClientRegistration) {
        redisDriver.addSetForSingle(CLIENT_REGISTRATION_KEY, registration)
    }

    override suspend fun removeRegistration(registrationId: String) {
        registrations.remove(registrationId)
    }

    override fun findByRegistrationId(registrationId: String?): Mono<ClientRegistration> {
        return Mono.justOrEmpty(registrations[registrationId])
    }
}

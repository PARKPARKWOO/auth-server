package com.example.auth.domain.repository

import com.example.auth.business.service.dto.ClientRegistrationInfoDto
import com.example.auth.domain.repository.redis.RedisDriver
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

interface DynamicReactiveClientRegistrationRepository : ReactiveClientRegistrationRepository {
    suspend fun addRegistration(registration: ClientRegistration)

    suspend fun updateRegistration(previousValue: ClientRegistration, currentValue: ClientRegistration)

    suspend fun removeRegistration(registrationId: String)
}

@Component("DynamicReactiveClientRegistrationAdapter")
class DynamicReactiveClientRegistrationAdapter(
    private val applicationOAuthProviderRepository: ApplicationOAuthProviderRepository,
    private val applicationRepository: ApplicationRepository,
    private val redisDriver: RedisDriver,
) : DynamicReactiveClientRegistrationRepository {
    private val registrations: ConcurrentMap<String, ClientRegistration> = ConcurrentHashMap()

    companion object {
        const val CLIENT_REGISTRATION_KEY = "client:registration"
    }

    // rdbms 직접 연동하면 나머진 필요없음
    suspend fun initialize(initialRegistrations: List<ClientRegistration>) {
        registrations.putAll(initialRegistrations.associateBy { it.registrationId })
        redisDriver.addListForRight(CLIENT_REGISTRATION_KEY, initialRegistrations)
    }

    override suspend fun addRegistration(registration: ClientRegistration) {
        registrations[registration.registrationId] = registration
        redisDriver.addSetForSingle(CLIENT_REGISTRATION_KEY, registration)
    }

    override suspend fun updateRegistration(previousValue: ClientRegistration, currentValue: ClientRegistration) {
        redisDriver.updateSetForSingle(
            key = CLIENT_REGISTRATION_KEY,
            previousValue = previousValue,
            currentValue = currentValue,
        )
    }

    override suspend fun removeRegistration(registrationId: String) {
        registrations.remove(registrationId)
    }

    override fun findByRegistrationId(registrationId: String?): Mono<ClientRegistration> {
        return if (registrationId != null) {
            mono {
                val oAuthProvider = applicationOAuthProviderRepository.findById(registrationId.toLong()).awaitSingle()
                val application = applicationRepository.findById(oAuthProvider.applicationId).awaitSingle()
                ClientRegistrationInfoDto(
                    id = oAuthProvider.id,
                    applicationName = application.name,
                    clientSecret = oAuthProvider.clientSecret,
                    clientId = oAuthProvider.clientId,
                    provider = oAuthProvider.provider,
                ).toClientRegistration()
            }
        } else {
            Mono.empty()
        }
    }
}

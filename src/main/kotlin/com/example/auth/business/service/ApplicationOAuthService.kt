package com.example.auth.business.service

import com.example.auth.business.service.dto.ClientRegistrationInfoDto
import com.example.auth.domain.repository.ApplicationOAuthProviderRepository
import com.example.auth.domain.repository.ApplicationRepository
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service

@Service
class ApplicationOAuthService(
    private val applicationOAuthProviderRepository: ApplicationOAuthProviderRepository,
    private val applicationRepository: ApplicationRepository,
) {
    suspend fun findClientRegistrationInfoDto(): MutableList<ClientRegistrationInfoDto> {
        val applicationList = applicationRepository.findAll().collectList().awaitSingle()
        val applicationOAuthList = applicationOAuthProviderRepository.findAll().collectList().awaitSingle()

        val clientRegistrationInfoDtoList = applicationOAuthList.mapNotNull { applicationOAuth ->
            val application = applicationList.find { it.id == applicationOAuth.applicationId }
            application?.let {
                ClientRegistrationInfoDto(
                    id = applicationOAuth.id,
                    redirectUri = applicationOAuth.redirectUri,
                    applicationName = application.name,
                    clientSecret = applicationOAuth.clientSecret,
                    clientId = applicationOAuth.clientId,
                    provider = applicationOAuth.provider,
                )
            }
        }.toMutableList()

        return clientRegistrationInfoDtoList
    }
}

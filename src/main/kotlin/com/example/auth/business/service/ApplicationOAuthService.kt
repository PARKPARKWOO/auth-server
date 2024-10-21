package com.example.auth.business.service

import com.example.auth.business.command.RegisterApplicationOAuthProviderCommand
import com.example.auth.business.exception.BusinessException
import com.example.auth.business.service.dto.ClientRegistrationInfoDto
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.domain.model.oauth.GoogleUser
import com.example.auth.domain.model.oauth.KakaoUser
import com.example.auth.domain.model.oauth.SocialLoginUser
import com.example.auth.domain.model.oauth.SocialProvider.GOOGLE
import com.example.auth.domain.model.oauth.SocialProvider.KAKAO
import com.example.auth.domain.repository.ApplicationOAuthProviderRepository
import com.example.auth.domain.repository.ApplicationRepository
import com.example.auth.domain.repository.DynamicReactiveClientRegistrationRepositoryImpl
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class ApplicationOAuthService(
    private val applicationOAuthProviderRepository: ApplicationOAuthProviderRepository,
    private val registerService: RegistrationService,
    private val applicationRepository: ApplicationRepository,
    private val applicationFinder: ApplicationFinder,
    private val dynamicReactiveClientRegistrationRepositoryImpl: DynamicReactiveClientRegistrationRepositoryImpl,
) {
    @PostConstruct
    fun initializeClientRegistration() {
        runBlocking {
            val applicationList: List<ClientRegistrationInfoDto> =
                runCatching {
                    findClientRegistrationInfoDto()
                }.getOrElse { emptyList<ClientRegistrationInfoDto>() }

            if (applicationList.isEmpty()) {
                dynamicReactiveClientRegistrationRepositoryImpl.initialize(
                    listOf(
                        CommonOAuth2Provider.GOOGLE.getBuilder("google")
                            .clientId("your-client-id")
                            .clientSecret("your-client-secret")
                            .build(),
                    ),
                )
            } else {
                val clientRegistrations: MutableList<ClientRegistration> = mutableListOf()
                applicationList.forEach {
                    clientRegistrations.add(it.toClientRegistration())
                }
                dynamicReactiveClientRegistrationRepositoryImpl.initialize(clientRegistrations)
            }
        }
    }

    suspend fun findClientRegistrationInfoDto(): MutableList<ClientRegistrationInfoDto> {
        val applicationList = applicationRepository.findAll().collectList().awaitSingle()
        val applicationOAuthList = applicationOAuthProviderRepository.findAll().collectList().awaitSingle()

        val clientRegistrationInfoDtoList = applicationOAuthList.mapNotNull { applicationOAuth ->
            val application = applicationList.find { it.id == applicationOAuth.applicationId }
            application?.let {
                ClientRegistrationInfoDto(
                    id = applicationOAuth.id,
//                    redirectUri = applicationOAuth.redirectUri,
                    applicationName = application.name,
                    clientSecret = applicationOAuth.clientSecret,
                    clientId = applicationOAuth.clientId,
                    provider = applicationOAuth.provider,
                )
            }
        }.toMutableList()

        return clientRegistrationInfoDtoList
    }

    suspend fun register(
        command: RegisterApplicationOAuthProviderCommand,
    ): Long {
        // TODO: application 등록 검증 필요
        val application = applicationRepository.findById(command.applicationId).awaitSingle()

        val registerApplicationOAuthProvider = registerService.registerApplicationOAuthProvider(command)
        val clientRegistrationInfoDto = ClientRegistrationInfoDto(
            id = registerApplicationOAuthProvider.id,
//            redirectUri = registerApplicationOAuthProvider.redirectUri,
            applicationName = application.name,
            clientSecret = registerApplicationOAuthProvider.clientSecret,
            clientId = registerApplicationOAuthProvider.clientId,
            provider = registerApplicationOAuthProvider.provider,
        )

        dynamicReactiveClientRegistrationRepositoryImpl.addRegistration(clientRegistrationInfoDto.toClientRegistration())
        return clientRegistrationInfoDto.id
    }

    suspend fun convertSocialUser(
        oAuth2User: OAuth2User,
        registrationId: String,
    ): SocialLoginUser {
        val oAuth2Provider =
            applicationOAuthProviderRepository.findById(registrationId.toLong()).awaitSingle()
        val application = applicationFinder.findById(oAuth2Provider.applicationId)
            ?: throw BusinessException(ErrorCode.NOT_FOUNT_APPLICATION, null)
        return when (oAuth2Provider.provider) {
            KAKAO -> KakaoUser(oAuth2User, application.redirectUrl)
            GOOGLE -> GoogleUser(oAuth2User, application.redirectUrl)
        }
    }
}

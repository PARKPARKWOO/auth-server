package com.example.auth.business.service.oauth

import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.business.exception.NotFoundRegistrationException
import com.example.auth.business.service.ApplicationOAuthService
import com.example.auth.business.service.RegistrationService
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.domain.model.oauth.SocialLoginUser
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.mono
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class CustomOAuthService(
    private val applicationOAuthService: ApplicationOAuthService,
    private val registrationService: RegistrationService,
) : ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> {
    override fun loadUser(userRequest: OAuth2UserRequest?): Mono<OAuth2User> {
        val defaultReactiveOAuth2UserService = DefaultReactiveOAuth2UserService()
        val loadUser = defaultReactiveOAuth2UserService.loadUser(userRequest)

        return loadUser.flatMap { oauth2User ->
            mono {
                coroutineScope {
                    val registrationId = userRequest?.clientRegistration?.registrationId
                        ?: throw NotFoundRegistrationException(ErrorCode.NOT_FOUND_REGISTRATION, null)
                    launch { application(oauth2User) }
                    val convertUserJob =
                        async { conventSocialUser(oauth2User, registrationId) }
                    val convertUser = convertUserJob.await()
                    launch { registerUserIfNotExist(convertUser) }
                    convertUser
                }
            }
        }
    }

    suspend fun conventSocialUser(user: OAuth2User, registrationId: String): SocialLoginUser = coroutineScope {
        applicationOAuthService.convertSocialUser(user, registrationId)
    }

    suspend fun application(user: OAuth2User) = coroutineScope {
    }

    suspend fun registerUserIfNotExist(user: SocialLoginUser) = coroutineScope {
        val registerUserCommand = RegisterUserCommand(
            email = user.getEmail(),
            password = "",
            socialId = user.getId(),
            provider = user.getProvider(),
        )
        registrationService.registerUser(registerUserCommand)
    }
}

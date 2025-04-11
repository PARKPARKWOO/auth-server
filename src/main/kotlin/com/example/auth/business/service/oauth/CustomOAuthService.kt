package com.example.auth.business.service.oauth

import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.business.exception.NotFoundRegistrationException
import com.example.auth.business.facade.OAuthApplicationFacade
import com.example.auth.business.service.application.ApplicationOAuthService
import com.example.auth.business.service.user.EndUserFinder
import com.example.auth.business.service.RegistrationService
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.domain.model.oauth.SocialLoginUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import model.Role
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class CustomOAuthService(
    private val oAuthApplicationFacade: OAuthApplicationFacade,
) : ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> {
    override fun loadUser(userRequest: OAuth2UserRequest?): Mono<OAuth2User> {
        val defaultReactiveOAuth2UserService = DefaultReactiveOAuth2UserService()
        val loadUser = defaultReactiveOAuth2UserService.loadUser(userRequest)
        return loadUser.flatMap { oauth2User ->
            mono {
                val registrationId =
                    userRequest?.clientRegistration?.registrationId
                        ?: throw NotFoundRegistrationException(ErrorCode.NOT_FOUND_REGISTRATION, null)
                withContext(Dispatchers.IO) {
                    val (endUser, socialUser) = oAuthApplicationFacade.createEndUserIfNotExist(oauth2User, registrationId, userRequest.accessToken)
                    oAuthApplicationFacade.createApplicationUserIfNotExist(registrationId.toLong(), endUser.id)
                    socialUser
                }
            }
        }
    }
}

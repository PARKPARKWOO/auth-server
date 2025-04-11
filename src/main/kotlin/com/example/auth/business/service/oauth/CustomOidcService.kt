package com.example.auth.business.service.oauth

import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.business.exception.NotFoundRegistrationException
import com.example.auth.business.facade.OAuthApplicationFacade
import com.example.auth.business.service.application.ApplicationOAuthService
import com.example.auth.business.service.user.EndUserFinder
import com.example.auth.business.service.user.EndUserWriter
import com.example.auth.business.service.RegistrationService
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.domain.model.oauth.SocialLoginUser
import com.example.auth.domain.model.oauth.SocialProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.mono
import model.Role
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CustomOidcService(
    private val oAuthApplicationFacade: OAuthApplicationFacade,
) : ReactiveOAuth2UserService<OidcUserRequest, OidcUser> {
    override fun loadUser(userRequest: OidcUserRequest?): Mono<OidcUser> {
        val oidcReactiveOAuth2UserService = OidcReactiveOAuth2UserService()
        val loadUser = oidcReactiveOAuth2UserService.loadUser(userRequest)

        return loadUser.flatMap { oauth2User ->
            mono {
                val registrationId =
                    userRequest?.clientRegistration?.registrationId
                        ?: throw NotFoundRegistrationException(ErrorCode.NOT_FOUND_REGISTRATION, null)
                val (endUser, socialUser) = oAuthApplicationFacade.createEndUserIfNotExist(oauth2User, registrationId, userRequest.accessToken)
                oAuthApplicationFacade.createApplicationUserIfNotExist(registrationId.toLong(), endUser.id)
                socialUser
            }
        }
    }
}

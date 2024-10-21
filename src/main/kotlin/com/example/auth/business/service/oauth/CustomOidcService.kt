package com.example.auth.business.service.oauth

import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.business.exception.NotFoundRegistrationException
import com.example.auth.business.service.ApplicationOAuthService
import com.example.auth.business.service.EndUserFinder
import com.example.auth.business.service.RegistrationService
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.domain.model.oauth.SocialLoginUser
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.mono
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CustomOidcService(
    private val applicationOAuthService: ApplicationOAuthService,
    private val endUserFinder: EndUserFinder,
    private val registrationService: RegistrationService,
) : ReactiveOAuth2UserService<OidcUserRequest, OidcUser> {
    override fun loadUser(userRequest: OidcUserRequest?): Mono<OidcUser> {
        val oidcReactiveOAuth2UserService = OidcReactiveOAuth2UserService()
        val loadUser = oidcReactiveOAuth2UserService.loadUser(userRequest)
        return loadUser.flatMap { oauth2User ->
            mono {
                coroutineScope {
                    val registrationId = userRequest?.clientRegistration?.registrationId
                        ?: throw NotFoundRegistrationException(ErrorCode.NOT_FOUND_REGISTRATION, null)
                    launch {
                        application(oauth2User)
                    }
                    val convertUserJob = async { conventSocialUser(oauth2User, registrationId) }
                    val convertUser = convertUserJob.await()
                    launch { registerUserIfNotExist(convertUser) }
                    convertUser
                }
            }
        }
    }

    suspend fun conventSocialUser(user: OidcUser, registrationId: String): SocialLoginUser =
        coroutineScope {
            applicationOAuthService.convertSocialUser(user, registrationId)
        }

    suspend fun application(user: OAuth2User) = coroutineScope {
    }

    suspend fun registerUserIfNotExist(user: SocialLoginUser) = coroutineScope {
        val userEntity = endUserFinder.findBySocialIdAndProvider(
            socialId = user.getId(),
            provider = user.getProvider(),
        )
        if (userEntity == null) {
            val registerUserCommand = RegisterUserCommand(
                email = user.getEmail(),
                password = "",
                socialId = user.getId(),
                provider = user.getProvider(),
            )
            val id = registrationService.registerUser(registerUserCommand)
            user.setClaims(id)
        } else {
            user.setClaims(userEntity.id)
        }
    }
}

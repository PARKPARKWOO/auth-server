package com.example.auth.business.service.oauth

import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.business.exception.NotFoundRegistrationException
import com.example.auth.business.service.ApplicationOAuthService
import com.example.auth.business.service.EndUserFinder
import com.example.auth.business.service.RegistrationService
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.domain.model.oauth.SocialLoginUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitSingle
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
    private val applicationOAuthService: ApplicationOAuthService,
    private val registrationService: RegistrationService,
    private val endUserFinder: EndUserFinder,
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
                    launch { application(oauth2User) }
                    val convertUser =
                        async { conventSocialUser(oauth2User, registrationId, userRequest.accessToken) }.await()
                    launch { registerUserIfNotExist(convertUser) }
                    convertUser
                }
            }
        }
    }

    suspend fun conventSocialUser(
        user: OAuth2User,
        registrationId: String,
        oauthAccessToken: OAuth2AccessToken,
    ): SocialLoginUser =
        coroutineScope {
            applicationOAuthService.convertSocialUser(
                user,
                registrationId,
                oauthAccessToken.tokenValue,
                oauthAccessToken.expiresAt?.epochSecond ?: 0L
            )
        }

    suspend fun application(user: OAuth2User) =
        coroutineScope {
        }

    suspend fun registerUserIfNotExist(user: SocialLoginUser) =
        coroutineScope {
            val userEntity =
                endUserFinder.findBySocialIdAndProvider(
                    socialId = user.getId(),
                    provider = user.getProvider(),
                )
            if (userEntity == null) {
                val registerUserCommand =
                    RegisterUserCommand(
                        email = user.getEmail(),
                        password = "",
                        socialId = user.getId(),
                        provider = user.getProvider(),
                        name = user.name,
                    )
                val createUserEntity = registrationService.registerUser(registerUserCommand)
                user.setClaims(createUserEntity.id, Role.from(createUserEntity.role))
            } else {
                user.setClaims(userEntity.id, Role.from(userEntity.role))
            }
        }
}

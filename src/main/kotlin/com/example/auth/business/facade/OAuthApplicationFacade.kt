package com.example.auth.business.facade

import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.business.exception.BusinessException
import com.example.auth.business.service.RegistrationService
import com.example.auth.business.service.application.ApplicationOAuthService
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.business.service.user.EndUserFinder
import com.example.auth.business.service.user.EndUserWriter
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.domain.entity.user.User
import com.example.auth.domain.model.oauth.SocialLoginUser
import com.example.auth.domain.model.oauth.SocialProvider
import kotlinx.coroutines.coroutineScope
import model.Role
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component

@Component
class OAuthApplicationFacade(
    private val applicationService: ApplicationService,
    private val endUserFinder: EndUserFinder,
    private val endUserWriter: EndUserWriter,
    private val registrationService: RegistrationService,
    private val applicationOAuthService: ApplicationOAuthService,
) {
    suspend fun createApplicationUserIfNotExist(
        registrationId: Long,
        userId: String,
    ) = coroutineScope {
        val applicationOauth = applicationOAuthService.findById(registrationId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_REGISTRATION, null)
        val applicationId = applicationOauth.applicationId
        val applicationUser = applicationService.getApplicationUser(applicationId, userId)
        if (applicationUser == null) {
            val defaultAuthority = applicationService.getLowLevelApplicationAuthority(applicationId)
            applicationService.createApplicationUser(applicationId, userId, defaultAuthority.id)
        }
    }

    suspend fun createEndUserIfNotExist(user: OAuth2User, registrationId: String, oauthAccessToken: OAuth2AccessToken): Pair<User, SocialLoginUser> =
        coroutineScope {
            val socialUser = applicationOAuthService.convertSocialUser(
                user,
                registrationId,
                oauthAccessToken.tokenValue,
                oauthAccessToken.expiresAt?.epochSecond ?: 0L
            )
            val endUser = registerUserIfNotExist(socialUser)
            Pair(endUser, socialUser)
        }

    // kakao는 application 마다 id가 달라 Email 로 구분해야 한다. 고유한 유저 식별이 안됨
    private suspend fun registerUserIfNotExist(user: SocialLoginUser): User {
        val userEntity =
            when (user.getProvider()) {
                SocialProvider.KAKAO -> {
                    endUserFinder.findByEmailAndProvider(
                        provider = user.getProvider(),
                        email = user.email,
                    )
                }

                else -> {
                    endUserFinder.findBySocialIdAndProvider(
                        socialId = user.getId(),
                        provider = user.getProvider(),
                    )
                }
            }

        val endUser = if (userEntity == null) {
            val createUser = save(user)
            user.setClaims(createUser.id, Role.from(createUser.role))
            createUser
        } else {
            user.setClaims(userEntity.id, Role.from(userEntity.role))
            endUserWriter.update(userEntity, user)
            userEntity
        }
        return endUser
    }

    private suspend fun save(user: SocialLoginUser): User {
        val registerUserCommand = RegisterUserCommand.from(user)
        return registrationService.registerUser(registerUserCommand)
    }
}
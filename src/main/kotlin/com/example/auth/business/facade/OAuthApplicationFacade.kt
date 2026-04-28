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
    /** Application.id (String) + provider 조합으로 ApplicationOAuthProvider 조회 */
    suspend fun findApplicationOAuthProvider(applicationId: String, provider: SocialProvider) =
        applicationOAuthService.findByApplicationIdAndProvider(applicationId, provider)

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

    /**
     * 핫픽스(K-2): 이전 구현은 카카오를 무조건 email 로 매칭했는데, `KakaoUser.getEmail()` 의
     * 잘못된 추출(K-1) 과 결합해 `email="null"` 인 다수 사용자가 동일 EndUser 로 통합되는 사고가
     * 발생했다 (모바일 앱 카카오 로그인 흐름 = `/api/v1/auth/oauth/token`).
     *
     * 카카오는 application 마다 user_id 가 달라 `socialId + provider` 만으로는 cross-app 통합이
     * 불가능하다. 따라서 hybrid 매칭:
     *   - 카카오 + **검증된** email 있음 → email 매칭 (cross-app 통합 의도 유지)
     *   - 카카오 + email 없음/미인증 → socialId+provider 로 fallback (같은 app 내 정체 보장,
     *     cross-app 통합은 포기 — 0195f0b7 같은 통합 사고 방지)
     *   - 그 외 provider → socialId+provider (이전과 동일)
     *
     * `KakaoUser.getEmail()` 은 K-1 에서 `is_email_valid && is_email_verified` 인 경우만
     * 정상 email 을 반환하고 그 외엔 빈 문자열을 반환하므로, 인증 안 된 email 위조 매칭은 차단된다.
     */
    private suspend fun registerUserIfNotExist(user: SocialLoginUser): User {
        val userEntity = findExistingEndUser(user)

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

    private suspend fun findExistingEndUser(user: SocialLoginUser): User? {
        if (user.getProvider() == SocialProvider.KAKAO) {
            val verifiedEmail = user.email.takeIf { it.isNotBlank() }
            if (verifiedEmail != null) {
                endUserFinder.findByEmailAndProvider(
                    provider = SocialProvider.KAKAO,
                    email = verifiedEmail,
                )?.let { return it }
            }
        }
        return endUserFinder.findBySocialIdAndProvider(
            socialId = user.getId(),
            provider = user.getProvider(),
        )
    }

    private suspend fun save(user: SocialLoginUser): User {
        val registerUserCommand = RegisterUserCommand.from(user)
        return registrationService.registerUser(registerUserCommand)
    }
}
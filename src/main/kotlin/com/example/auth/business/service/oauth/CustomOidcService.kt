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
        
        // userInfo 호출 시도 (카카오의 경우 OIDC 표준 형식이 아니어서 실패할 수 있음)
        val loadUser = oidcReactiveOAuth2UserService.loadUser(userRequest)
            .onErrorResume { error ->
                // userInfo 호출 실패 시 ID Token만 사용하여 OidcUser 생성
                // 카카오의 userInfo 응답이 OIDC 표준 형식이 아니어서 발생하는 오류 처리
                if (error.message?.contains("invalid_user_info_response") == true || 
                    error.message?.contains("user_info") == true) {
                    // ID Token만으로 OidcUser 생성
                    val idToken = userRequest?.idToken
                    if (idToken != null) {
                        // ID Token의 클레임을 attributes로 변환
                        val attributes = mutableMapOf<String, Any>()
                        idToken.claims.forEach { (key, value) ->
                            attributes[key] = value ?: ""
                        }
                        
                        // OidcUser 생성 (userInfo 없이 ID Token만 사용)
                        Mono.just(
                            org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(
                                idToken.authorities,
                                idToken,
                                null // userInfo는 null로 설정
                            )
                        )
                    } else {
                        Mono.error(error)
                    }
                } else {
                    Mono.error(error)
                }
            }

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

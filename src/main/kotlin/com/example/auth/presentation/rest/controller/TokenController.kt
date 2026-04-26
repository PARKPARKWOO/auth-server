package com.example.auth.presentation.rest.controller

import annotation.AuthenticationUser
import com.example.auth.business.exception.BusinessException
import com.example.auth.business.facade.OAuthApplicationFacade
import com.example.auth.business.service.JwtTokenService
import com.example.auth.business.service.oauth.KakaoTokenService
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.domain.model.oauth.SocialProvider
import com.example.auth.presentation.rest.controller.request.OAuthTokenRequest
import com.example.auth.presentation.rest.controller.request.ReissueTokenRequest
import dto.JwtResponseDto
import dto.Passport
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.woo.http.SucceededApiResponseBody
import java.time.Instant

@RestController
@RequestMapping("/api/v1/auth")
class TokenController (
    private val jwtTokenService: JwtTokenService,
    private val kakaoTokenService: KakaoTokenService,
    private val oAuthApplicationFacade: OAuthApplicationFacade,
){
    @PostMapping("/oauth/token")
    suspend fun getToken(
        @RequestBody request: OAuthTokenRequest,
    ): SucceededApiResponseBody<JwtResponseDto> {
        // 1. 요청 파라미터 검증 (applicationId = Application.id, String)
        val applicationId = request.applicationId
            ?: throw BusinessException(ErrorCode.NOT_FOUND_REGISTRATION, null)
        
        // 2. Application OAuth Provider 조회 (applicationId + provider 조합)
        val applicationOAuthProvider = oAuthApplicationFacade.findApplicationOAuthProvider(applicationId, request.provider)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_REGISTRATION, null)
        
        // 3. 카카오 토큰 검증 및 사용자 정보 조회
        val oAuth2User = when (request.provider) {
            SocialProvider.KAKAO -> kakaoTokenService.getUserInfo(request.accessToken)
            else -> throw BusinessException(
                ErrorCode.NOT_FOUND_REGISTRATION,
                IllegalArgumentException("지원하지 않는 OAuth 제공자입니다: ${request.provider}")
            )
        }
        
        // 5. OAuth2AccessToken 객체 생성 (실제 만료 시간은 카카오 API 응답에서 가져와야 하지만,
        //    현재 KakaoTokenService에서는 만료 시간을 반환하지 않으므로 기본값 사용)
        // TODO: 카카오 API 응답에서 실제 만료 시간을 추출하여 사용하도록 개선 필요
        val oAuth2AccessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            request.accessToken,
            Instant.now(),
            Instant.now().plusSeconds(3600) // 기본값: 1시간 (실제로는 카카오에서 받은 만료 시간 사용 권장)
        )
        
        // 6. 사용자 생성/조회 및 SocialLoginUser 생성 (registrationId = ApplicationOAuthProvider.id)
        val (endUser, socialUser) = oAuthApplicationFacade.createEndUserIfNotExist(
            oAuth2User,
            applicationOAuthProvider.id.toString(),
            oAuth2AccessToken
        )
        
        // 7. Application User 생성/조회
        oAuthApplicationFacade.createApplicationUserIfNotExist(applicationOAuthProvider.id, endUser.id.toString())
        
        // 8. JWT 발급
        val jwtResponse = jwtTokenService.buildAndSave(socialUser.getClaims())
        
        return SucceededApiResponseBody(jwtResponse)
    }

    @PostMapping("/token/revoke")
    suspend fun revokeToken(
        @AuthenticationUser
        @Parameter(hidden = true)
        passport: Passport,
        response: ServerHttpResponse,
    ): SucceededApiResponseBody<Unit> {
        jwtTokenService.revoke(response, passport)
        return SucceededApiResponseBody.succeed()
    }

    @PostMapping("/token/reissue")
    suspend fun reissueToken(
        @RequestBody
        request: ReissueTokenRequest,
    ): SucceededApiResponseBody<JwtResponseDto> {
        // RFC 6819 Refresh Token Rotation: 새 access + 새 refresh 모두 발급해 반환.
        // gRPC 경로(TokenGrpcController)와 동일하게 rotationToken 사용해 일관성 확보.
        val response = jwtTokenService.rotationToken(request.refreshToken)
        return SucceededApiResponseBody(response)
    }
}
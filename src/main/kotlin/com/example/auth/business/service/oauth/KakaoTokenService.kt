package com.example.auth.business.service.oauth

import com.example.auth.business.exception.BusinessException
import com.example.auth.common.http.error.ErrorCode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import exception.AuthException
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class KakaoTokenService {
    private val webClient = WebClient.builder()
        .baseUrl("https://kapi.kakao.com")
        .build()
    
    private val objectMapper = jacksonObjectMapper()

    /**
     * 카카오 Access Token으로 사용자 정보를 조회하여 OAuth2User로 반환
     * 
     * 주의: 이 메서드는 카카오 API 응답을 그대로 OAuth2User attributes로 변환합니다.
     * 실제 SocialLoginUser 변환은 ApplicationOAuthService.convertSocialUser에서
     * KakaoUser를 통해 처리되므로, 여기서는 카카오 응답 구조를 그대로 유지합니다.
     */
    suspend fun getUserInfo(accessToken: String): OAuth2User {
        try {
            val userInfo: Map<String, Any> = webClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .bodyToMono<String>()
                .map { responseBody ->
                    objectMapper.readValue<Map<String, Any>>(responseBody)
                }
                .awaitSingle()
            
            // 카카오 API 응답을 그대로 OAuth2User attributes로 사용
            // KakaoUser가 이미 이 구조를 처리할 수 있도록 함
            // id 필드를 name attribute로도 설정 (KakaoUser.getId()에서 사용)
            val attributes = userInfo.toMutableMap()
            attributes["sub"] = userInfo["id"]?.toString() ?: ""
            
            return DefaultOAuth2User(
                emptyList(),
                attributes,
                "id" // name attribute key
            )
        } catch (e: Exception) {
            throw AuthException(exception.ErrorCode.FORBIDDEN, e)
        }
    }
}


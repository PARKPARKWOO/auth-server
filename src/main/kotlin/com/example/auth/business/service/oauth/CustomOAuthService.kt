package com.example.auth.business.service.oauth

import com.example.auth.domain.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CustomOAuthService(
    private val userRepository: UserRepository,
) : ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> {
    override fun loadUser(userRequest: OAuth2UserRequest?): Mono<OAuth2User> {
        val defaultReactiveOAuth2UserService = DefaultReactiveOAuth2UserService()

        return defaultReactiveOAuth2UserService.loadUser(userRequest)
        // 실제 OAuth2.0 통신 하는 과정
        // 우리 서비스 회원가입 한 적 있는지 확인
    }
}

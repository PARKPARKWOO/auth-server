package com.example.auth.business.service

import com.example.auth.domain.repository.UserRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

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

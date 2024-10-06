package com.example.auth.business.service

import com.example.auth.domain.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuthService(
    private val userRepository: UserRepository,
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    override fun loadUser(userRequest: OAuth2UserRequest?): OAuth2User {
        val defaultOAuth2UserService = DefaultOAuth2UserService()

        // 실제 OAuth2.0 통신 하는 과정
        val oauth2User = defaultOAuth2UserService.loadUser(userRequest)
        // 우리 서비스 회원가입 한 적 있는지 확인

        TODO()
    }
}

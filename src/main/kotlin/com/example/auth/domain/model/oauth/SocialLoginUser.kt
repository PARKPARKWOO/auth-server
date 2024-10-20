package com.example.auth.domain.model.oauth

import org.springframework.security.oauth2.core.user.OAuth2User

interface SocialLoginUser : OAuth2User {
    fun getId(): String
    fun getNickname(): String
    fun getEmail(): String
    fun getClaims(): Map<String, Any>
    fun getProvider(): SocialProvider
}

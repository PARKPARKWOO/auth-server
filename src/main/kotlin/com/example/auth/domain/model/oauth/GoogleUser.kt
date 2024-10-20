package com.example.auth.domain.model.oauth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

data class GoogleUser(
    val oAuth2User: OAuth2User,
): SocialLoginUser {
    override fun getId(): String {
        TODO("Not yet implemented")
    }

    override fun getNickname(): String {
        TODO("Not yet implemented")
    }

    override fun getEmail(): String {
        TODO("Not yet implemented")
    }

    override fun getClaims(): Map<String, Any> {
        TODO("Not yet implemented")
    }

    override fun getProvider(): SocialProvider {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getAttributes(): MutableMap<String, Any> {
        TODO("Not yet implemented")
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        TODO("Not yet implemented")
    }
}

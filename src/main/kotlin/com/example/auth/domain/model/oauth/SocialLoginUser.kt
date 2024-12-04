package com.example.auth.domain.model.oauth

import com.example.auth.domain.model.application.RedirectType
import model.Role
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

interface SocialLoginUser : OAuth2User, OidcUser {
    val redirectUrl: String
    val redirectType: RedirectType
    fun getId(): String
    fun getNickname(): String?
    override fun getEmail(): String
    override fun getClaims(): Map<String, Any>
    fun getProvider(): SocialProvider
    fun setClaims(userId: String, role: Role)
}

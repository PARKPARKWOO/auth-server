package com.example.auth.domain.model.oauth

import com.example.auth.common.constants.AuthConstants
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

data class GoogleUser(
    val oAuth2User: OAuth2User,
    override val redirectUrl: String,
) : SocialLoginUser {
    private lateinit var userId: String
    override fun getId(): String = oAuth2User.name

    override fun getNickname(): String = oAuth2User.attributes["name"].toString()

    override fun getEmail(): String = oAuth2User.attributes["email"].toString()
    override fun getClaims(): Map<String, Any> {
        val claims = mutableMapOf<String, Any>()
        claims[AuthConstants.USER_ID] = userId
//        claims[AuthConstants.USER_ROLE] = role
        return claims
    }

    override fun getProvider(): SocialProvider = SocialProvider.GOOGLE
    override fun setClaims(userId: String) {
        this.userId = userId
    }

    override fun getName(): String {
        return oAuth2User.attributes["name"].toString()
    }

    override fun getAttributes(): MutableMap<String, Any> {
        return oAuth2User.attributes
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return oAuth2User.authorities
    }

    override fun getUserInfo(): OidcUserInfo? {
        val oidcUserInfo = oAuth2User as? OidcUser
        return oidcUserInfo?.userInfo
    }

    override fun getIdToken(): OidcIdToken? {
        val oidcUserInfo = oAuth2User as? OidcUser
        return oidcUserInfo?.idToken
    }
}

package com.example.auth.domain.model.oauth

import constant.AuthConstant
import model.Role
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

abstract class AbstractSocialUser(
    val oAuthUser: OAuth2User,
) : SocialLoginUser {
    private lateinit var endUserId: String

    private lateinit var role: Role

    fun getEndUserId(): String = endUserId

    override fun getClaims(): Map<String, Any> {
        val claims = mutableMapOf<String, Any>()
        claims[AuthConstant.USER_ID] = endUserId
        claims[AuthConstant.USER_ROLE] = role
        claims[AuthConstant.APPLICATION_ID] = signInApplicationId
        return claims
    }

    override fun setClaims(userId: String, role: Role) {
        this.endUserId = userId
        this.role = role
    }

    override fun retrieveOauthAccessToken(): String = idToken?.tokenValue ?: oauthAccessToken

    override fun retrieveOauthExpiresAt(): Long = idToken?.expiresAt?.epochSecond ?: oauthExpiresAt

    override fun getAttributes(): MutableMap<String, Any> {
        return oAuthUser.attributes
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return oAuthUser.authorities
    }

    override fun getUserInfo(): OidcUserInfo? {
        val oidcUserInfo = oAuthUser as? OidcUser
        return oidcUserInfo?.userInfo
    }

    override fun getIdToken(): OidcIdToken? {
        val oidcUserInfo = oAuthUser as? OidcUser
        return oidcUserInfo?.idToken
    }
}
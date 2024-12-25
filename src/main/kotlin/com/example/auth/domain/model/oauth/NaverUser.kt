package com.example.auth.domain.model.oauth

import com.example.auth.common.constants.AuthConstants
import com.example.auth.domain.model.application.RedirectType
import model.Role
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

data class NaverUser(
    val oAuth2User: OAuth2User,
    override val redirectUrl: String,
    override val redirectType: RedirectType
) : SocialLoginUser {
    private lateinit var userId: String

    private lateinit var role: Role

    private fun getProperties(): LinkedHashMap<*, *>? = oAuth2User.attributes["response"] as? LinkedHashMap<*, *>

    override fun getId(): String = oAuth2User.name

    override fun getNickname(): String? {
        val oidcUser = oAuth2User as? OidcUser
        return oidcUser?.nickName
            ?: getProperties()?.get("name")?.toString()
            ?: oAuth2User.attributes["nickname"]?.toString()
    }

    override fun getEmail(): String = getProperties()?.get("email").toString()

    override fun getClaims(): Map<String, Any> {
        val claims = mutableMapOf<String, Any>()
        claims[AuthConstants.USER_ID] = userId
//        claims[AuthConstants.USER_ROLE] = role
        return claims
    }

    override fun getProvider(): SocialProvider = SocialProvider.KAKAO

    override fun setClaims(
        userId: String,
        role: Role,
    ) {
        this.userId = userId
        this.role = role
    }

    override fun getName(): String = oAuth2User.name

    override fun getAttributes(): MutableMap<String, Any> = oAuth2User.attributes

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = oAuth2User.authorities

    override fun getUserInfo(): OidcUserInfo? {
        val oidcUser = oAuth2User as? OidcUser
        return oidcUser?.userInfo
    }

    override fun getIdToken(): OidcIdToken? {
        val oidcUser = oAuth2User as? OidcUser
        return oidcUser?.idToken
    }
}

package com.example.auth.domain.model.oauth

import com.example.auth.domain.model.application.RedirectType
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

data class GoogleUser(
    val oAuth2User: OAuth2User,
    override val redirectUrl: String,
    override val redirectType: RedirectType,
    override val oauthAccessToken: String,
    override val oauthExpiresAt: Long,
    override val signInApplicationId: String,
) : AbstractSocialUser(oAuth2User) {
    override fun getId(): String = oAuthUser.name

    override fun getNickname(): String = oAuthUser.attributes["name"].toString()

    override fun getEmail(): String = oAuthUser.attributes["email"].toString()

    override fun getProvider(): SocialProvider = SocialProvider.GOOGLE

    override fun getName(): String {
        return oAuthUser.attributes["name"].toString()
    }
}

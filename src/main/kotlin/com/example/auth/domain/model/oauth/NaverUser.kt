package com.example.auth.domain.model.oauth

import com.example.auth.domain.model.application.RedirectType
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

data class NaverUser(
    val oAuth2User: OAuth2User,
    override val redirectUrl: String,
    override val redirectType: RedirectType,
    override val oauthAccessToken: String,
    override val oauthExpiresAt: Long,
    override val signInApplicationId: String
) : AbstractSocialUser(oAuth2User) {
    private fun getProperties(): LinkedHashMap<*, *>? = oAuth2User.attributes["response"] as? LinkedHashMap<*, *>

    override fun getId(): String = getProperties()?.get("id").toString() ?: oAuth2User.name

    override fun getNickname(): String? {
        val oidcUser = oAuth2User as? OidcUser
        return oidcUser?.nickName
            ?: getProperties()?.get("name")?.toString()
            ?: oAuth2User.attributes["nickname"]?.toString()
    }

    override fun getEmail(): String = getProperties()?.get("email").toString()

    override fun getProvider(): SocialProvider = SocialProvider.NAVER

    override fun getName(): String = getProperties()?.get("name").toString()
}

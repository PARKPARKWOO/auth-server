package com.example.auth.domain.model.oauth

import com.example.auth.domain.model.application.RedirectType
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User

data class KakaoUser(
    val oAuth2User: OAuth2User,
    override val redirectUrl: String,
    override val redirectType: RedirectType,
    override val oauthAccessToken: String,
    override val oauthExpiresAt: Long,
    override val signInApplicationId: String,
) : AbstractSocialUser(oAuth2User) {
    private fun getProperties(): LinkedHashMap<*, *>? = oAuth2User.attributes["properties"] as? LinkedHashMap<*, *>

    private fun getKakaoAccount(): LinkedHashMap<*, *>? = oAuth2User.attributes["kakao_account"] as? LinkedHashMap<*, *>

    override fun getId(): String {
        // userInfo가 있으면 id 필드 사용, 없으면 ID Token의 sub 사용
        return oAuth2User.attributes["id"]?.toString()
            ?: oAuth2User.attributes["sub"]?.toString()
            ?: oAuth2User.name
    }

    override fun getNickname(): String? {
        val oidcUser = oAuth2User as? OidcUser
        return oidcUser?.nickName
            ?: getProperties()?.get("nickname")?.toString()
            ?: oAuth2User.attributes["nickname"]?.toString()
    }

    override fun getEmail(): String = attributes["email"].toString()

    override fun getProvider(): SocialProvider = SocialProvider.KAKAO

    override fun getName(): String = oAuth2User.name
}

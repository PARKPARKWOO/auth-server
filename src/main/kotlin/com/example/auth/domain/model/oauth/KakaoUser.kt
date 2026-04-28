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
            ?: getKakaoAccount()?.let { it["profile"] as? LinkedHashMap<*, *> }?.get("nickname")?.toString()
            ?: getProperties()?.get("nickname")?.toString()
            ?: oAuth2User.attributes["nickname"]?.toString()
    }

    /**
     * 핫픽스(K-1): 이전 구현은 `attributes["email"].toString()` 이었는데 카카오 응답은
     * `kakao_account.email` 에 email 을 둔다. 최상위 attributes 에는 email 이 없어 거의 항상
     * "null" 문자열이 반환됐고, 이게 EndUser 매칭 키로 쓰여 다수 사용자가 동일 EndUser 로 통합되었다.
     *
     * 정확한 path 로 추출하고 카카오에서 인증/검증된 email 만 신뢰.
     * 인증 안 된 email 은 빈 문자열로 처리해 호출부가 명시적으로 매칭 거절하도록 한다.
     */
    override fun getEmail(): String {
        val kakaoAccount = getKakaoAccount() ?: return ""
        val email = kakaoAccount["email"]?.toString()?.takeIf { it.isNotBlank() } ?: return ""
        val isValid = kakaoAccount["is_email_valid"] as? Boolean ?: false
        val isVerified = kakaoAccount["is_email_verified"] as? Boolean ?: false
        return if (isValid && isVerified) email else ""
    }

    override fun getProvider(): SocialProvider = SocialProvider.KAKAO

    /**
     * 핫픽스(K-1b): 이전엔 `oAuth2User.name` 을 그대로 반환했는데 nameAttributeKey="id" 라서
     * 카카오 user id (숫자) 가 name 으로 저장됐다. 닉네임이 있으면 그걸 우선 사용.
     */
    override fun getName(): String = getNickname() ?: oAuth2User.name
}

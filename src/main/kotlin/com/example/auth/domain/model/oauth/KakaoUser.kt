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
    /**
     * 카카오 /v2/user/me 응답 (Kakao Developers 공식 문서 기준):
     *  - 최상위 `id` (Long): 카카오 user_id, 앱별 다름
     *  - `kakao_account` (object, 동의 시): profile, name, email, phone_number, ci 등
     *  - `kakao_account.profile.nickname` (string, 프로필 동의 시): 가장 일반적인 닉네임 source
     *  - `kakao_account.name` (string, 이름 동의 시): 별도 동의 항목, 덜 흔함
     *  - `kakao_account.email` (string, 이메일 동의 시) + `is_email_valid` + `is_email_verified`
     *  - `properties.nickname` (legacy, 일부 OldApp 만)
     *
     * 동의 안 한 항목은 응답에 필드 자체가 없으므로 null safe 추출 필수.
     */
    private fun getProperties(): Map<*, *>? = oAuth2User.attributes["properties"] as? Map<*, *>

    private fun getKakaoAccount(): Map<*, *>? = oAuth2User.attributes["kakao_account"] as? Map<*, *>

    private fun kakaoAccountProfile(): Map<*, *>? = getKakaoAccount()?.get("profile") as? Map<*, *>

    override fun getId(): String =
        oAuth2User.attributes["id"]?.toString()
            ?: oAuth2User.attributes["sub"]?.toString()  // OIDC ID Token sub
            ?: oAuth2User.name

    /**
     * 닉네임 우선순위 (공식 응답 기준):
     *  1. OIDC ID Token nickname claim (oauth2Login + profile scope 흐름)
     *  2. `kakao_account.profile.nickname` (REST API + 프로필 동의)
     *  3. `kakao_account.name` (이름 동의 별도)
     *  4. `properties.nickname` (legacy)
     */
    override fun getNickname(): String? {
        (oAuth2User as? OidcUser)?.nickName?.takeIf { it.isNotBlank() }?.let { return it }
        kakaoAccountProfile()?.get("nickname")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        getKakaoAccount()?.get("name")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        getProperties()?.get("nickname")?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }

    /**
     * 핫픽스(K-1): 이전 구현은 `attributes["email"].toString()` 이었는데 카카오 응답은
     * `kakao_account.email` 에 email 을 둔다. 최상위 attributes 에는 email 이 없어 거의 항상
     * `"null"` 문자열이 반환됐고, 이게 EndUser 매칭 키로 쓰여 다수 사용자가 동일 EndUser 로 통합되었다.
     *
     * 공식 응답 path (`kakao_account.email`) 로 추출 + 카카오가 검증한 (is_email_valid &&
     * is_email_verified) 이메일만 신뢰. 그 외엔 빈 문자열 — 호출부가 명시적으로 매칭 거절하도록.
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
     * 카카오 user_id 숫자가 name 으로 저장됐다. 닉네임 추출 후, 카카오 user_id (REST) 또는
     * sub (OIDC) 로 fallback. **절대 null 반환하지 않음** — 호출부 NPE 방지.
     */
    override fun getName(): String =
        getNickname()
            ?: oAuth2User.attributes["id"]?.toString()
            ?: oAuth2User.attributes["sub"]?.toString()
            ?: ""
}

package com.example.auth.business.service.dto

import com.example.auth.domain.model.oauth.SocialProvider
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType

data class ClientRegistrationInfoDto(
    val id: Long,
    val redirectUri: String,
    val applicationName: String,
    val clientSecret: String?,
    val clientId: String,
    val provider: SocialProvider,
) {
    companion object {
        //        const val GOOGLE_AUTHORIZATION_URI = ""
        // https://www.googleapis.com/oauth2/v4/token 둘 중 하나임
        const val GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token"
        const val GOOGLE_USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo"
        const val KAKAO_AUTHORIZATION_URL = "https://kauth.kakao.com/oauth/authorize"
        const val KAKAO_TOKEN_URI = "https://kauth.kakao.com/oauth/token"
        const val KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me"
    }

    fun toClientRegistration(): ClientRegistration = when (this.provider) {
        SocialProvider.GOOGLE -> createGoogleClientRegistration()
        SocialProvider.KAKAO -> createKakaoClientRegistration()
    }

    private fun createKakaoClientRegistration(): ClientRegistration =
        ClientRegistration.withRegistrationId(id.toString())
            .clientId(clientId)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .tokenUri(KAKAO_TOKEN_URI)
            .redirectUri(redirectUri)
            .userInfoUri(KAKAO_USER_INFO_URI)
            .authorizationUri(KAKAO_AUTHORIZATION_URL)
            .clientName(SocialProvider.KAKAO.clientNamePrefix + applicationName)
            .userNameAttributeName("id")
            .scope("")
            .build()

    private fun createGoogleClientRegistration(): ClientRegistration = CommonOAuth2Provider.GOOGLE
        .getBuilder(id.toString())
        .redirectUri(redirectUri)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .clientName(SocialProvider.GOOGLE.clientNamePrefix + applicationName)
        .build()
}

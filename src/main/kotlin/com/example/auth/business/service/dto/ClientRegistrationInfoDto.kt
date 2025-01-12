package com.example.auth.business.service.dto

import com.example.auth.domain.model.oauth.SocialProvider
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames

data class ClientRegistrationInfoDto(
    val id: Long,
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

        // KAKAO
        const val KAKAO_AUTHORIZATION_URL = "https://kauth.kakao.com/oauth/authorize"
        const val KAKAO_TOKEN_URI = "https://kauth.kakao.com/oauth/token"
        const val KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me"
        const val KAKAO_JWK_URI = "https://kauth.kakao.com/.well-known/jwks.json"
        const val KAKAO_METADATA_URI = "https://kauth.kakao.com/.well-known/openid-configuration"
        const val KAKAO_ISSUER_URI = "https://kauth.kakao.com"

        // Naver
        const val NAVER_AUTHORIZATION_URL = "https://nid.naver.com/oauth2.0/authorize"
        const val NAVER_TOKEN_URI = "https://nid.naver.com/oauth2.0/token"
        const val NAVER_USER_INFO_URI = "https://openapi.naver.com/v1/nid/me"
        const val NAVER_JWK_URI = "https://nid.naver.com/.well-known/jwks.json"
        const val NAVER_ISSUER_URI = "https://nid.naver.com"

        // BAND
        const val BAND_AUTHORIZATION_URL = "https://auth.band.us/oauth2/authorize"
        const val BAND_TOKEN_URI = "https://auth.band.us/oauth2/token"
        const val BAND_USER_INFO_URI = "https://openapi.band.us/v2/profile"

        private const val BASE_URL = "https://woo-auth.duckdns.org"

//        private const val BASE_URL = "http://localhost:8080"
        const val DEFAULT_REDIRECT_URL = "$BASE_URL/{action}/oauth2/code/{registrationId}"
//        const val DEFAULT_REDIRECT_URL = "http://localhost:8080/oauth/authoirzation/kakao"
    }

    fun toClientRegistration(): ClientRegistration =
        when (this.provider) {
            SocialProvider.GOOGLE -> createGoogleClientRegistration()
            SocialProvider.KAKAO -> createKakaoClientRegistration()
            SocialProvider.NAVER -> createNaverClientRegistration()
            SocialProvider.BAND -> createBandClientRegistration()
        }

    private fun createKakaoClientRegistration(): ClientRegistration =
        ClientRegistration
            .withRegistrationId(id.toString())
//            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientId(clientId)
            .scope("openid")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .tokenUri(KAKAO_TOKEN_URI)
            .redirectUri(DEFAULT_REDIRECT_URL)
            .userInfoUri(KAKAO_USER_INFO_URI)
            .issuerUri(KAKAO_ISSUER_URI)
            .authorizationUri(KAKAO_AUTHORIZATION_URL)
            .jwkSetUri(KAKAO_JWK_URI)
            .clientName(SocialProvider.KAKAO.clientNamePrefix + applicationName)
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .build()

    private fun createGoogleClientRegistration(): ClientRegistration =
        CommonOAuth2Provider.GOOGLE
            .getBuilder(id.toString())
//        .redirectUri(DEFAULT_REDIRECT_URL)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientName(SocialProvider.GOOGLE.clientNamePrefix + applicationName)
            .build()

    private fun createNaverClientRegistration(): ClientRegistration =
        ClientRegistration
            .withRegistrationId(id.toString())
//            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientId(clientId)
            .clientSecret(clientSecret)
            // 네이버는 지원하지 않는다.
//            .scope("openid")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .tokenUri(NAVER_TOKEN_URI)
            .redirectUri(DEFAULT_REDIRECT_URL)
            .userInfoUri(NAVER_USER_INFO_URI)
            .issuerUri(NAVER_ISSUER_URI)
            .authorizationUri(NAVER_AUTHORIZATION_URL)
//            .jwkSetUri(NAVER_JWK_URI)
            .clientName(SocialProvider.NAVER.clientNamePrefix + applicationName)
            .userNameAttributeName("response")
            .build()

    private fun createBandClientRegistration(): ClientRegistration =
        ClientRegistration
            .withRegistrationId(id.toString())
//            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientId(clientId)
            .clientSecret(clientSecret)
            // 네이버는 지원하지 않는다.
//            .scope("openid")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .tokenUri(BAND_TOKEN_URI)
            .redirectUri(DEFAULT_REDIRECT_URL)
            .userInfoUri(BAND_USER_INFO_URI)
            .authorizationUri(BAND_AUTHORIZATION_URL)
//            .jwkSetUri(NAVER_JWK_URI)
            .clientName(SocialProvider.BAND.clientNamePrefix + applicationName)
            .userNameAttributeName("result_data")
            .build()
}

package com.example.auth.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames

@Configuration
class OAuthConfig(
    @Value("\${pocket-fit.oauth.kakao.client-id}")
    val kakaoClientIdForPocketFit: String,
    @Value("\${find-my-pet.oauth.kakao.client-id}")
    val kakaoClientIdForFindMyPet: String,
    @Value("\${find-my-pet.oauth.google.client-id}")
    val googleClientIdForFindMyPet: String,
    @Value("\${find-my-pet.oauth.google.client-secret}")
    val googleClientSecretForFindMyPet: String,
) {
    companion object {
        //        const val GOOGLE_AUTHORIZATION_URI = ""
        // https://www.googleapis.com/oauth2/v4/token 둘 중 하나임
        const val GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token"
        const val GOOGLE_USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo"

        //        const val KAKAO_AUTHORIZATION_URL = "https://kapi.kakao.com"
        const val KAKAO_TOKEN_URI = "https://kauth.kakao.com/oauth/token"
        const val KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me"
    }

    @Bean
    fun clientRegistrationRepository(): ClientRegistrationRepository {
        val googleForFindMyPet = CommonOAuth2Provider.GOOGLE.getBuilder("google-find-my-pet")
            .clientId(googleClientIdForFindMyPet)
            .clientSecret(googleClientSecretForFindMyPet)
            .redirectUri()
            .clientName("Google for Find My Pet")
            .build()

        val kakaoForFindMyPet = ClientRegistration.withRegistrationId("kakao-find-my-pey")
            .clientId(kakaoClientIdForFindMyPet)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
//            .authorizationUri(KAKAO_AUTHORIZATION_URL)
            .tokenUri(KAKAO_TOKEN_URI)
            .redirectUri()
            .userInfoUri(KAKAO_USER_INFO_URI)
            .clientName("Kakao for Find My Pet")
            .scope("openid", "profile", "email")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .build()
        val kakaoForPocketFit = ClientRegistration.withRegistrationId("kakao-pocket-fit")
            .clientId(kakaoClientIdForPocketFit)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
//            .authorizationUri(KAKAO_AUTHORIZATION_URL)
            .tokenUri(KAKAO_TOKEN_URI)
            .redirectUri()
            .userInfoUri(KAKAO_USER_INFO_URI)
            .userNameAttributeName()
            .clientName("Kakao for Pocket Fit")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .scope("openid", "profile", "email")
            .build()
        val clientRegistrations: List<ClientRegistration> =
            listOf(kakaoForPocketFit, kakaoForFindMyPet, googleForFindMyPet)
        return InMemoryClientRegistrationRepository(clientRegistrations)
    }
}

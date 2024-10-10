package com.example.auth.common.config

import com.example.auth.business.service.ApplicationOAuthService
import com.example.auth.business.service.dto.ClientRegistrationInfoDto
import com.example.auth.business.service.oauth.CustomOAuthService
import com.example.auth.business.service.oauth.CustomOidcService
import com.example.auth.domain.model.oauth.SocialProvider
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.authentication.DelegatingReactiveAuthenticationManager
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeReactiveAuthenticationManager
import org.springframework.security.oauth2.client.authentication.OAuth2LoginReactiveAuthenticationManager
import org.springframework.security.oauth2.client.endpoint.JwtBearerGrantRequest
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.TokenExchangeGrantRequest
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveJwtBearerTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveRefreshTokenTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveTokenExchangeTokenResponseClient
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeReactiveAuthenticationManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class OAuthConfig(
    private val customOidcService: CustomOidcService,
    private val customOAuthService: CustomOAuthService,
    private val applicationOAuthService: ApplicationOAuthService,
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

//    @PostConstruct
//    fun generateClientRegistrationRepository() {
//        val applicationList: List<ClientRegistrationInfoDto> =
//            runBlocking {
//                runCatching {
//                    applicationOAuthService.findClientRegistrationInfoDto()
//                }.getOrElse { emptyList<ClientRegistrationInfoDto>() }
//            }
//        if (applicationList.isEmpty()) {
//            inMemoryClientRegistrationRepository = InMemoryClientRegistrationRepository(
//                CommonOAuth2Provider.GOOGLE.getBuilder("1").clientId("id").clientSecret("se").build(),
//            )
//            return
//        }
//        val clientRegistrations: MutableList<ClientRegistration> = mutableListOf()
//        applicationList.forEach {
//            val clientRegistration = when (it.provider) {
//                SocialProvider.GOOGLE -> {
//                    createGoogleClientRegistration(
//                        id = it.id,
//                        redirectUri = it.redirectUri,
//                        applicationName = it.applicationName,
//                        clientSecret = it.clientSecret!!,
//                        clientId = it.clientId,
//                    )
//                }
//
//                SocialProvider.KAKAO -> {
//                    createKakaoClientRegistration(
//                        id = it.id,
//                        redirectUri = it.redirectUri,
//                        clientId = it.clientId,
//                        applicationName = it.applicationName,
//                    )
//                }
//            }
//            clientRegistrations.add(clientRegistration)
//        }
//        inMemoryClientRegistrationRepository = InMemoryClientRegistrationRepository(clientRegistrations)
//    }

    private fun createGoogleClientRegistration(
        id: Long,
        redirectUri: String,
        applicationName: String,
        clientSecret: String,
        clientId: String,
    ): ClientRegistration = CommonOAuth2Provider.GOOGLE
        .getBuilder(id.toString())
        .redirectUri(redirectUri)
        .clientId(clientId)
        .clientSecret(clientSecret)
        .clientName(SocialProvider.GOOGLE.clientNamePrefix + applicationName)
        .build()

    private fun createKakaoClientRegistration(
        id: Long,
        redirectUri: String,
        applicationName: String,
        clientId: String,
    ): ClientRegistration =
        ClientRegistration.withRegistrationId(id.toString())
            .clientId(clientId)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .tokenUri(KAKAO_TOKEN_URI)
            .redirectUri(redirectUri)
            .userInfoUri(KAKAO_USER_INFO_URI)
//            .userNameAttributeName()
            .clientName(SocialProvider.KAKAO.clientNamePrefix + applicationName)
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .scope("openid", "profile", "email")
            .build()

    @Bean
    fun clientRegistrationRepository(): InMemoryReactiveClientRegistrationRepository {
        val applicationList: List<ClientRegistrationInfoDto> =
            runBlocking {
                runCatching {
                    applicationOAuthService.findClientRegistrationInfoDto()
                }.getOrElse { emptyList<ClientRegistrationInfoDto>() }
            }
        return if (applicationList.isEmpty()) {
            InMemoryReactiveClientRegistrationRepository(
                CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId("your-client-id")
                    .clientSecret("your-client-secret")
                    .build(),
            )
        } else {
            val clientRegistrations: MutableList<ClientRegistration> = mutableListOf()
            applicationList.forEach {
                val clientRegistration = when (it.provider) {
                    SocialProvider.GOOGLE -> createGoogleClientRegistration(
                        id = it.id,
                        redirectUri = it.redirectUri,
                        applicationName = it.applicationName,
                        clientSecret = it.clientSecret!!,
                        clientId = it.clientId,
                    )

                    SocialProvider.KAKAO -> createKakaoClientRegistration(
                        id = it.id,
                        redirectUri = it.redirectUri,
                        clientId = it.clientId,
                        applicationName = it.applicationName,
                    )
                }
                clientRegistrations.add(clientRegistration)
            }
            InMemoryReactiveClientRegistrationRepository(clientRegistrations)
        }
    }

    @Bean
    @Primary
    fun oauth2LoginAuthenticationManager(): DelegatingReactiveAuthenticationManager {
        val oidcAuthorizationCodeReactiveAuthenticationManager = OidcAuthorizationCodeReactiveAuthenticationManager(
            authorizationCodeAccessTokenResponseClient(),
            customOidcService,
        )
        val oAuth2LoginReactiveAuthenticationManager =
            OAuth2LoginReactiveAuthenticationManager(authorizationCodeAccessTokenResponseClient(), customOAuthService)
        return DelegatingReactiveAuthenticationManager(
            oidcAuthorizationCodeReactiveAuthenticationManager,
            oAuth2LoginReactiveAuthenticationManager,
        )
    }

    @Bean
    fun oauth2AuthorizationCodeReactiveAuthenticationManager(): OAuth2AuthorizationCodeReactiveAuthenticationManager {
        return OAuth2AuthorizationCodeReactiveAuthenticationManager(authorizationCodeAccessTokenResponseClient())
    }

    @Bean
    fun authorizationCodeAccessTokenResponseClient(): ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {
        val accessTokenResponseClient = WebClientReactiveAuthorizationCodeTokenResponseClient()
        accessTokenResponseClient.setWebClient(webClient())
        return accessTokenResponseClient
    }

    @Bean
    fun refreshTokenAccessTokenResponseClient(): ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> {
        val accessTokenResponseClient = WebClientReactiveRefreshTokenTokenResponseClient()
        accessTokenResponseClient.setWebClient(webClient())
        return accessTokenResponseClient
    }

    @Bean
    fun clientCredentialsAccessTokenResponseClient(): ReactiveOAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> {
        val accessTokenResponseClient = WebClientReactiveClientCredentialsTokenResponseClient()
        accessTokenResponseClient.setWebClient(webClient())
        return accessTokenResponseClient
    }

    @Bean
    fun jwtBearerAccessTokenResponseClient(): ReactiveOAuth2AccessTokenResponseClient<JwtBearerGrantRequest> {
        val accessTokenResponseClient = WebClientReactiveJwtBearerTokenResponseClient()
        accessTokenResponseClient.setWebClient(webClient())
        return accessTokenResponseClient
    }

    @Bean
    fun tokenExchangeAccessTokenResponseClient(): ReactiveOAuth2AccessTokenResponseClient<TokenExchangeGrantRequest> {
        val accessTokenResponseClient = WebClientReactiveTokenExchangeTokenResponseClient()
        accessTokenResponseClient.setWebClient(webClient())
        return accessTokenResponseClient
    }

    @Bean
    fun webClient(): WebClient {
        return WebClient.create()
    }
}

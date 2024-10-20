package com.example.auth.common.config

import com.example.auth.business.service.ApplicationOAuthService
import com.example.auth.business.service.dto.ClientRegistrationInfoDto
import com.example.auth.business.service.oauth.CustomOAuthService
import com.example.auth.business.service.oauth.CustomOidcService
import com.example.auth.domain.repository.DynamicReactiveClientRegistrationRepositoryImpl
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
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class OAuthConfig(
    private val customOidcService: CustomOidcService,
    private val customOAuthService: CustomOAuthService,
) {
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

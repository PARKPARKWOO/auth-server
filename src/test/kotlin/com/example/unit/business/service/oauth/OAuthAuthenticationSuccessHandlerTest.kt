package com.example.unit.business.service.oauth

import com.example.auth.business.service.JwtTokenService
import com.example.auth.business.service.oauth.OAuthAuthenticationSuccessHandler
import com.example.auth.domain.model.application.RedirectType
import com.example.auth.domain.model.oauth.GoogleUser
import dto.JwtResponseDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import model.Role
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OAuthAuthenticationSuccessHandlerTest {
    @Test
    fun `cookie redirect leaves CORS response headers to gateway`() {
        val jwtTokenService = mockk<JwtTokenService>()
        coEvery { jwtTokenService.buildAndSave(any()) } returns
            JwtResponseDto(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                accessTokenExpiresIn = 60_000,
                refreshTokenExpiresIn = 120_000,
            )
        val oauthUser =
            mockk<OAuth2User> {
                every { attributes } returns
                    mutableMapOf<String, Any>(
                        "name" to "Forest user",
                        "email" to "user@example.com",
                    )
                every { authorities } returns mutableListOf()
                every { name } returns "provider-user"
            }
        val socialUser =
            GoogleUser(
                oAuth2User = oauthUser,
                redirectUrl = "https://jbforest.platformholder.site/",
                redirectType = RedirectType.REDIRECT_WITH_COOKIE,
                oauthAccessToken = "oauth-access-token",
                oauthExpiresAt = 120_000,
                signInApplicationId = "forest",
            ).apply {
                setClaims("forest-user", Role.ROLE_USER)
            }
        val exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get("/login/oauth2/code/google")
                    .header(HttpHeaders.ORIGIN, "https://jbforest.platformholder.site")
                    .build(),
            )
        val webFilterExchange = WebFilterExchange(exchange, WebFilterChain { Mono.empty() })
        val authentication = UsernamePasswordAuthenticationToken(socialUser, "n/a", emptyList())

        OAuthAuthenticationSuccessHandler(jwtTokenService)
            .onAuthenticationSuccess(webFilterExchange, authentication)
            .block()

        assertEquals(HttpStatus.FOUND, exchange.response.statusCode)
        assertEquals(URI.create("https://jbforest.platformholder.site/"), exchange.response.headers.location)
        assertTrue(exchange.response.cookies.containsKey("accessToken"))
        assertTrue(exchange.response.cookies.containsKey("refreshToken"))
        assertFalse(exchange.response.headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        assertFalse(exchange.response.headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
    }
}

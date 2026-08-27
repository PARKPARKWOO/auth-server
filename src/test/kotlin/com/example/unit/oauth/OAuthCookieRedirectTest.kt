package com.example.unit.oauth

import com.example.auth.business.service.CookieService
import com.example.auth.business.service.JwtTokenService
import com.example.auth.business.service.oauth.OAuthAuthenticationSuccessHandler
import com.example.auth.common.config.AuthCookieProperties
import com.example.auth.domain.model.application.RedirectType
import com.example.auth.domain.model.oauth.AbstractSocialUser
import dto.JwtResponseDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OAuthCookieRedirectTest {
    @Test
    fun `redirect with cookie sets HttpOnly JWT cookies without query tokens`() {
        val jwt =
            JwtResponseDto(
                accessToken = "access-token-sentinel",
                refreshToken = "refresh-token-sentinel",
                accessTokenExpiresIn = 1_500,
                refreshTokenExpiresIn = 2_500,
            )
        val jwtTokenService = mockk<JwtTokenService>()
        coEvery { jwtTokenService.buildAndSave(any()) } returns jwt
        val cookieService = cookieService(productionProperties(), MockEnvironment())
        val handler = OAuthAuthenticationSuccessHandler(jwtTokenService, cookieService)
        val socialUser = mockk<AbstractSocialUser>()
        every { socialUser.getClaims() } returns emptyMap()
        every { socialUser.redirectUrl } returns "https://mirror-view.platformholder.site/cbt"
        every { socialUser.redirectType } returns RedirectType.REDIRECT_WITH_COOKIE
        val authentication = mockk<Authentication>()
        every { authentication.principal } returns socialUser
        val exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest
                    .get("/oauth2/callback")
                    .header("Origin", "https://mirror-view.platformholder.site")
                    .build(),
            )
        val webFilterExchange = WebFilterExchange(exchange, WebFilterChain { Mono.empty() })

        handler.onAuthenticationSuccess(webFilterExchange, authentication).block()

        val access = exchange.response.cookies.getFirst("accessToken")!!
        val refresh = exchange.response.cookies.getFirst("refreshToken")!!
        assertTrue(access.isHttpOnly)
        assertTrue(refresh.isHttpOnly)
        assertEquals(Duration.ofMillis(1_500), access.maxAge)
        assertEquals(Duration.ofMillis(2_500), refresh.maxAge)
        assertEquals("https://mirror-view.platformholder.site/cbt", exchange.response.headers.location.toString())
        assertFalse(exchange.response.headers.location.toString().contains(jwt.accessToken))
        assertFalse(exchange.response.headers.location.toString().contains(jwt.refreshToken))
    }

    @Test
    fun `create and clear use an identical production cookie tuple`() {
        val service = cookieService(productionProperties(), MockEnvironment())
        val issueExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
        val clearExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())

        service.addJwtCookies(
            issueExchange.response,
            JwtResponseDto("access", "refresh", 1_500, 2_500),
        )
        service.clearJwtCookies(clearExchange.response)

        listOf("accessToken", "refreshToken").forEach { name ->
            val issued = issueExchange.response.cookies.getFirst(name)!!
            val cleared = clearExchange.response.cookies.getFirst(name)!!
            assertEquals(issued.name, cleared.name)
            assertEquals(issued.domain, cleared.domain)
            assertEquals(issued.path, cleared.path)
            assertEquals(issued.isHttpOnly, cleared.isHttpOnly)
            assertEquals(issued.isSecure, cleared.isSecure)
            assertEquals(issued.sameSite, cleared.sameSite)
            assertEquals(".platformholder.site", issued.domain)
            assertEquals("/", issued.path)
            assertTrue(issued.isHttpOnly)
            assertTrue(issued.isSecure)
            assertEquals("None", issued.sameSite)
            assertEquals(Duration.ZERO, cleared.maxAge)
        }
    }

    @Test
    fun `local and test cookie tuples omit domain and use lax insecure values`() {
        listOf("local", "test").forEach { profile ->
            val environment = MockEnvironment().apply { setActiveProfiles(profile) }
            val service =
                cookieService(
                    AuthCookieProperties(domain = " ", secure = false, sameSite = "Lax"),
                    environment,
                )
            val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())

            service.addJwtCookies(exchange.response, JwtResponseDto("access", "refresh", 1_000, 2_000))

            val cookie = exchange.response.cookies.getFirst("accessToken")!!
            assertNull(cookie.domain)
            assertFalse(cookie.isSecure)
            assertEquals("Lax", cookie.sameSite)
        }
    }

    private fun productionProperties() =
        AuthCookieProperties(
            domain = ".platformholder.site",
            secure = true,
            sameSite = "None",
        )

    private fun cookieService(
        properties: AuthCookieProperties,
        environment: MockEnvironment,
    ) = CookieService(
        properties = properties,
        environment = environment,
    )
}

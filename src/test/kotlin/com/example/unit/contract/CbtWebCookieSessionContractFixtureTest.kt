package com.example.unit.contract

import com.example.auth.business.service.CookieService
import com.example.auth.common.config.AuthCookieProperties
import dto.JwtResponseDto
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CbtWebCookieSessionContractFixtureTest {
    private val decoder = ClosedCbtWebSessionContractDecoder()
    private val fixture by lazy {
        requireNotNull(javaClass.classLoader.getResource("contracts/cbt-web-cookie-session.json")).readText()
    }

    @Test
    fun `closed decoder rejects unknown missing wrongly typed and invalid enum fields`() {
        listOf(
            fixture.replaceFirst("{\"version\":1", "{\"unexpected\":true,\"version\":1"),
            fixture.replaceFirst("\"cookies\":", "\"missingCookies\":"),
            fixture.replaceFirst("\"httpOnly\":true", "\"httpOnly\":\"true\""),
            fixture.replaceFirst("\"sameSite\":\"None\"", "\"sameSite\":\"INVALID\""),
            fixture.replaceFirst(
                "\"authTransport\":{\"web\":\"HTTP_ONLY_COOKIE\",\"mobile\":\"BEARER\",\"rotationOwner\":\"GATEWAY\",\"webDirectReissue\":false}",
                "\"authTransport\":null",
            ),
            fixture.replaceFirst(
                "\"allowedWebOrigins\":[\"https://mirror-view.platformholder.site\",\"http://localhost:5173\",\"http://127.0.0.1:4173\"]",
                "\"allowedWebOrigins\":null",
            ),
            fixture.replaceFirst(
                "\"allowedWebOrigins\":[\"https://mirror-view.platformholder.site\"",
                "\"allowedWebOrigins\":[null,\"https://mirror-view.platformholder.site\"",
            ),
            fixture.replaceFirst("{\"version\":1", "{\"version\":1,\"version\":1"),
        ).forEach { mutation -> assertFails { decoder.decode(mutation) } }
    }

    @Test
    fun `fixture matches every independently declared security field and backend operation`() {
        val contract = decoder.decode(fixture)

        assertEquals(expectedSecurityContract(), contract.withoutOperations())
        assertEquals(expectedOperations(), contract.operationHeaders.entries())
    }

    @Test
    fun `fixture auth cookie fields match actual Auth properties and CookieService behavior`() {
        val auth = decoder.decode(fixture).cookies.auth
        val productionProperties = bindAuthCookieProperties()
        val localProperties = bindAuthCookieProperties("application-local.yml")
        assertEquals(auth.names, listOf(productionProperties.accessTokenName, productionProperties.refreshTokenName))
        assertEquals(auth.path, productionProperties.path)
        assertEquals(auth.httpOnly, productionProperties.httpOnly)
        assertEquals(auth.production.domain, productionProperties.domain)
        assertEquals(auth.production.secure, productionProperties.secure)
        assertEquals(auth.production.sameSite.wire, productionProperties.sameSite)
        assertEquals(auth.names, listOf(localProperties.accessTokenName, localProperties.refreshTokenName))
        assertEquals(auth.path, localProperties.path)
        assertEquals(auth.httpOnly, localProperties.httpOnly)
        assertEquals(auth.localTest.domain, localProperties.domain?.trim()?.takeIf { it.isNotEmpty() })
        assertEquals(auth.localTest.secure, localProperties.secure)
        assertEquals(auth.localTest.sameSite.wire, localProperties.sameSite)
        val production = CookieService(productionProperties, MockEnvironment())
        val issuedExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
        val clearedExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
        production.addJwtCookies(issuedExchange.response, JwtResponseDto("access", "refresh", 1_500, 2_500))
        production.clearJwtCookies(clearedExchange.response)

        assertEquals(auth.names, issuedExchange.response.cookies.keys.toList())
        val expectedIssueMaxAge =
            mapOf(
                productionProperties.accessTokenName to Duration.ofMillis(1_500),
                productionProperties.refreshTokenName to Duration.ofMillis(2_500),
            )
        auth.names.forEach { name ->
            val issued = requireNotNull(issuedExchange.response.cookies.getFirst(name))
            val cleared = requireNotNull(clearedExchange.response.cookies.getFirst(name))
            assertEquals(auth.path, issued.path)
            assertEquals(auth.httpOnly, issued.isHttpOnly)
            assertEquals(auth.production.domain, issued.domain)
            assertEquals(auth.production.secure, issued.isSecure)
            assertEquals(auth.production.sameSite.wire, issued.sameSite)
            assertEquals(expectedIssueMaxAge.getValue(name), issued.maxAge)
            assertEquals(auth.path, cleared.path)
            assertEquals(auth.httpOnly, cleared.isHttpOnly)
            assertEquals(auth.production.domain, cleared.domain)
            assertEquals(auth.production.secure, cleared.isSecure)
            assertEquals(auth.production.sameSite.wire, cleared.sameSite)
            assertEquals(Duration.ZERO, cleared.maxAge)
        }

        val localEnvironment = MockEnvironment().apply { setActiveProfiles("test") }
        val localExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())
        CookieService(localProperties, localEnvironment)
            .addJwtCookies(localExchange.response, JwtResponseDto("access", "refresh", 1_500, 2_500))
        val localCookie = requireNotNull(localExchange.response.cookies.getFirst(auth.names.first()))
        assertNull(localCookie.domain)
        assertFalse(localCookie.isSecure)
        assertEquals(auth.localTest.sameSite.wire, localCookie.sameSite)
        assertTrue(localCookie.isHttpOnly)
    }

    private fun expectedSecurityContract() =
        SecurityContractProjection(
            1,
            AuthTransportContract(WebAuthTransport.HTTP_ONLY_COOKIE, MobileAuthTransport.BEARER, RotationOwner.GATEWAY, false),
            listOf("https://mirror-view.platformholder.site", "http://localhost:5173", "http://127.0.0.1:4173"),
            CookiesContract(
                AuthCookiesContract(listOf("accessToken", "refreshToken"), "/", true, CookieProfileContract(".platformholder.site", true, SameSiteContract.NONE), CookieProfileContract(null, false, SameSiteContract.LAX), MaxAgeContract(IssueMaxAge.JWT_EXPIRES_IN_MILLIS, ClearMaxAge.ZERO)),
                CsrfCookieContract("XSRF-TOKEN", "X-XSRF-TOKEN", "/", false, CookieProfileContract(".platformholder.site", true, SameSiteContract.NONE), CookieProfileContract(null, false, SameSiteContract.LAX)),
            ),
        )

    private fun expectedOperations() =
        linkedMapOf(
            "publicCatalogRead" to op(methods = listOf(ContractHttpMethod.GET), paths = listOf("/api/v1/cbt/exams", "/api/v1/cbt/exams/{examSlug}", "/api/v1/cbt/exams/{examSlug}/papers", "/api/v1/cbt/papers/{paperId}"), required = emptyList(), optional = emptyList(), mobile = emptyList()),
            "publicQuestionPreviewRead" to op(method = ContractHttpMethod.GET, path = "/api/v1/cbt/questions", auth = ContractAuth.NONE, client = ContractClient.WEB_ONLY, required = emptyList(), optional = emptyList()),
            "publicAssetRead" to op(method = ContractHttpMethod.GET, path = "/api/v1/cbt/assets/{assetId}", auth = ContractAuth.NONE, required = emptyList(), optional = emptyList(), mobile = emptyList()),
            "attemptCreate" to op(method = ContractHttpMethod.POST, path = "/api/v1/cbt/attempts", auth = ContractAuth.OPTIONAL, required = listOf("Idempotency-Key"), optional = emptyList(), web = listOf("X-XSRF-TOKEN"), mobile = listOf("X-CBT-Installation-Id")),
            "guestAttemptRead" to op(methods = listOf(ContractHttpMethod.GET), paths = listOf("/api/v1/cbt/attempts/{id}", "/api/v1/cbt/attempts/{id}/result"), auth = ContractAuth.OPTIONAL, guest = listOf("X-CBT-Attempt-Token"), mobile = emptyList()),
            "guestAttemptSaveCheckSubmit" to op(methods = listOf(ContractHttpMethod.PUT, ContractHttpMethod.POST), paths = listOf("/api/v1/cbt/attempts/{id}/answers/{attemptQuestionId}", "/api/v1/cbt/attempts/{id}/answers/{attemptQuestionId}/check", "/api/v1/cbt/attempts/{id}/submit"), auth = ContractAuth.OPTIONAL, guest = listOf("X-CBT-Attempt-Token"), web = listOf("X-XSRF-TOKEN"), mobile = listOf("X-CBT-Installation-Id")),
            "attemptClaim" to op(method = ContractHttpMethod.POST, path = "/api/v1/cbt/attempts/{id}/claim", auth = ContractAuth.REQUIRED, required = listOf("Idempotency-Key"), optional = listOf("X-CBT-Attempt-Token"), web = listOf("X-XSRF-TOKEN"), mobile = emptyList()),
            "userRead" to op(methods = listOf(ContractHttpMethod.GET), paths = listOf("/api/v1/cbt/me/attempts", "/api/v1/cbt/me/wrong-answers"), auth = ContractAuth.REQUIRED, required = emptyList(), optional = emptyList(), mobile = emptyList()),
            "wrongAnswerResolve" to op(method = ContractHttpMethod.POST, path = "/api/v1/cbt/me/wrong-answers/{questionId}/resolve", auth = ContractAuth.REQUIRED, required = emptyList(), optional = emptyList(), web = listOf("X-XSRF-TOKEN"), mobile = emptyList()),
            "wrongAnswerRetry" to op(method = ContractHttpMethod.POST, path = "/api/v1/cbt/me/wrong-answers/retry-attempt", auth = ContractAuth.REQUIRED, required = listOf("Idempotency-Key"), optional = emptyList(), web = listOf("X-XSRF-TOKEN"), mobile = emptyList()),
            "publicPrintableCreate" to op(method = ContractHttpMethod.POST, path = "/api/v1/cbt/printable-sets", auth = ContractAuth.OPTIONAL, client = ContractClient.WEB_ONLY, required = listOf("Idempotency-Key"), optional = emptyList(), web = listOf("X-XSRF-TOKEN")),
            "wrongPrintableCreate" to op(method = ContractHttpMethod.POST, path = "/api/v1/cbt/me/wrong-answers/printable-set", auth = ContractAuth.REQUIRED, client = ContractClient.WEB_ONLY, required = listOf("Idempotency-Key"), optional = emptyList(), web = listOf("X-XSRF-TOKEN")),
            "guestPrintableRead" to op(method = ContractHttpMethod.GET, paths = listOf("/api/v1/cbt/printable-sets/{id}", "/api/v1/cbt/printable-sets/{id}/preview"), auth = ContractAuth.OPTIONAL, client = ContractClient.WEB_ONLY, guest = listOf("X-CBT-Printable-Token")),
        )

    private fun op(
        method: ContractHttpMethod? = null,
        methods: List<ContractHttpMethod>? = null,
        path: String? = null,
        paths: List<String>? = null,
        auth: ContractAuth? = null,
        client: ContractClient? = null,
        required: List<String>? = null,
        optional: List<String>? = null,
        guest: List<String>? = null,
        web: List<String> = emptyList(),
        mobile: List<String>? = null,
    ) = OperationContract(method, methods, path, paths, auth, client, required, optional, guest, web, mobile)
}

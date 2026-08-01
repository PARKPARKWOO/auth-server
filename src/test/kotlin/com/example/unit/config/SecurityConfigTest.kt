package com.example.unit.config

import com.example.auth.business.service.JwtTokenService
import com.example.auth.common.config.SecurityConfig
import com.example.auth.domain.repository.DynamicReactiveClientRegistrationRepository
import com.example.auth.presentation.rest.filter.LoggingFilter
import constant.AuthConstant
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.DelegatingReactiveAuthenticationManager
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeReactiveAuthenticationManager
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import java.util.Base64
import kotlin.test.assertEquals

class SecurityConfigTest {
    private val securityConfig =
        SecurityConfig(
            oauth2LoginAuthenticationManager = mockk<DelegatingReactiveAuthenticationManager>(),
            oauth2ClientAuthenticationManager = mockk<OAuth2AuthorizationCodeReactiveAuthenticationManager>(),
            dynamicReactiveClientRegistrationRepository = mockk<DynamicReactiveClientRegistrationRepository>(),
            jwtTokenService = mockk<JwtTokenService>(),
            accessTokenSecretKeyString = Base64.getEncoder().encodeToString(ByteArray(64) { 1 }),
            loggingFilter = mockk<LoggingFilter>(),
        )

    @Test
    fun `scalar user role claim becomes one granted authority`() {
        assertEquals(listOf("ROLE_ADMIN"), authoritiesFor("ROLE_ADMIN"))
    }

    @Test
    fun `list user role claim remains supported`() {
        assertEquals(listOf("ROLE_ADMIN", "ROLE_USER"), authoritiesFor(listOf("ROLE_ADMIN", "ROLE_USER")))
    }

    @Test
    fun `missing user role claim creates no authority`() {
        assertEquals(emptyList(), authoritiesFor(null))
    }

    @Test
    fun `unsupported user role claim creates no authority`() {
        assertEquals(emptyList(), authoritiesFor(123))
    }

    @Test
    fun `mixed user role collection creates no authority`() {
        assertEquals(emptyList(), authoritiesFor(listOf("ROLE_ADMIN", 123)))
    }

    private fun authoritiesFor(userRoleClaim: Any?): List<String> {
        val jwtConverterMethod = SecurityConfig::class.java.getDeclaredMethod("jwtConverter")
        jwtConverterMethod.isAccessible = true
        val converter = jwtConverterMethod.invoke(securityConfig) as ReactiveJwtAuthenticationConverterAdapter

        val jwtBuilder =
            Jwt
                .withTokenValue("test-token")
                .header("alg", "HS512")
                .subject("test-user")
        userRoleClaim?.let { jwtBuilder.claim(AuthConstant.USER_ROLE, it) }

        return converter
            .convert(jwtBuilder.build())!!
            .block()!!
            .authorities
            .map { it.authority }
    }
}

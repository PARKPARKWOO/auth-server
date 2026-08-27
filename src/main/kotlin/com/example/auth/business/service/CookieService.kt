package com.example.auth.business.service

import com.example.auth.common.config.AuthCookieProperties
import dto.JwtResponseDto
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CookieService(
    private val properties: AuthCookieProperties,
    environment: Environment,
) {
    private val domain = properties.domain?.trim()?.takeIf { it.isNotEmpty() }
    private val sameSite = properties.sameSite.trim()

    init {
        require(sameSite in ALLOWED_SAME_SITE_VALUES) {
            "auth.cookie.same-site must be one of Lax, Strict, or None"
        }
        val isLocalOrTest = environment.acceptsProfiles(Profiles.of("local", "test"))
        require(sameSite != "None" || properties.secure || isLocalOrTest) {
            "SameSite=None requires Secure=true outside local and test profiles"
        }
    }

    fun addJwtCookies(
        response: ServerHttpResponse,
        jwt: JwtResponseDto,
    ) {
        response.addCookie(issue(properties.accessTokenName, jwt.accessToken, jwt.accessTokenExpiresIn))
        response.addCookie(issue(properties.refreshTokenName, jwt.refreshToken, jwt.refreshTokenExpiresIn))
    }

    fun clearJwtCookies(response: ServerHttpResponse) {
        response.addCookie(clear(properties.accessTokenName))
        response.addCookie(clear(properties.refreshTokenName))
    }

    private fun issue(
        name: String,
        value: String,
        expiresInMillis: Long,
    ): ResponseCookie = build(name, value, Duration.ofMillis(expiresInMillis))

    private fun clear(name: String): ResponseCookie = build(name, "", Duration.ZERO)

    private fun build(
        name: String,
        value: String,
        maxAge: Duration,
    ): ResponseCookie {
        val builder =
            ResponseCookie
                .from(name, value)
                .httpOnly(properties.httpOnly)
                .secure(properties.secure)
                .path(properties.path)
                .maxAge(maxAge)
                .sameSite(sameSite)
        domain?.let(builder::domain)
        return builder.build()
    }

    private companion object {
        val ALLOWED_SAME_SITE_VALUES = setOf("Lax", "Strict", "None")
    }
}

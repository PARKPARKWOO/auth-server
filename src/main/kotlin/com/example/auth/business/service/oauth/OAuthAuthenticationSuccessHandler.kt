package com.example.auth.business.service.oauth

import com.example.auth.business.service.JwtTokenGenerator
import com.example.auth.business.service.dto.JwtResponseDto
import com.example.auth.domain.model.oauth.SocialLoginUser
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration

@Component
class OAuthAuthenticationSuccessHandler(
    private val jwtTokenGenerator: JwtTokenGenerator,
) : ServerAuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication?,
    ): Mono<Void> {
        return Mono.defer {
            val socialLoginUser = authentication?.principal as? SocialLoginUser
                ?: return@defer Mono.error<Void>(IllegalArgumentException("Authentication principal is not valid"))

            mono {
                runBlocking { generateJwtToken(socialLoginUser.getClaims()) }
            }.flatMap { jwtResponse ->
                webFilterExchange.exchange.response.apply {
                    statusCode = HttpStatus.FOUND
                    addCookie(createCookie("accessToken", jwtResponse.accessToken, jwtResponse.accessTokenExpiresIn))
                    addCookie(createCookie("refreshToken", jwtResponse.refreshToken, jwtResponse.refreshTokenExpiresIn))
                    headers.location = URI.create(socialLoginUser.redirectUrl)
                }
                Mono.empty()
            }
        }
    }

    private suspend fun generateJwtToken(claims: Map<String, Any>): JwtResponseDto {
        return jwtTokenGenerator.build(claims)
    }

    private fun createCookie(name: String, value: String, maxAge: Long): ResponseCookie {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
//            .secure(true)
            .path("/")
            .maxAge(Duration.ofMillis(maxAge))
            .sameSite("Strict")
            .build()
    }
}

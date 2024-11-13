package com.example.auth.business.service.oauth

import com.example.auth.business.service.JwtTokenGenerator
import com.example.auth.business.service.dto.JwtResponseDto
import com.example.auth.common.context.isMobileDevice
import com.example.auth.domain.model.oauth.SocialLoginUser
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.Authentication
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
                val response = webFilterExchange.exchange.response
                val jwtResponse = generateJwtToken(socialLoginUser.getClaims())
                response.sendJwtResponseAsRedirect(jwtResponse, socialLoginUser.redirectUrl)
            }.then()
        }
    }

    private suspend fun generateJwtToken(claims: Map<String, Any>): JwtResponseDto {
        return jwtTokenGenerator.build(claims)
    }

    private suspend fun ServerHttpResponse.sendJwtResponseAsRedirect(jwtResponse: JwtResponseDto, redirectUrl: String) {
        val isJson = isMobileDevice()
        if (isJson) {
            this.sendJwtResponseAsJson(jwtResponse)
        } else {
            this.sendJwtResponseAsCookie(jwtResponse)
            this.setRedirectConfiguration(redirectUrl)
        }
    }

    private suspend fun ServerHttpResponse.sendJwtResponseAsCookie(jwtResponse: JwtResponseDto) {
        this.apply {
            addCookie(createCookie("accessToken", jwtResponse.accessToken, jwtResponse.accessTokenExpiresIn))
            addCookie(createCookie("refreshToken", jwtResponse.refreshToken, jwtResponse.refreshTokenExpiresIn))
        }
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

    private suspend fun ServerHttpResponse.setRedirectConfiguration(redirectUrl: String) {
        this.headers.location = URI.create(redirectUrl)
        this.statusCode = HttpStatus.FOUND
    }

    private suspend fun ServerHttpResponse.sendJwtResponseAsJson(jwtResponse: JwtResponseDto) {
        val mapper = ObjectMapper()
        this.headers.contentType = MediaType.APPLICATION_JSON
        val jsonByte = mapper.writeValueAsBytes(jwtResponse)
        val buffer: DataBuffer = bufferFactory().wrap(jsonByte)

        this.writeWith(Mono.just(buffer)).awaitSingle()
    }
}

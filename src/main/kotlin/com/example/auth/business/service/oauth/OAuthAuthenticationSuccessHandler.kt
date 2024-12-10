package com.example.auth.business.service.oauth

import com.example.auth.business.service.JwtTokenGenerator
import com.example.auth.domain.model.application.RedirectType
import com.example.auth.domain.model.oauth.SocialLoginUser
import com.fasterxml.jackson.databind.ObjectMapper
import dto.JwtResponseDto
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
                response.sendJwtResponseAsRedirect(
                    jwtResponse = jwtResponse,
                    redirectUrl = socialLoginUser.redirectUrl,
                    redirectType = socialLoginUser.redirectType,
                )
            }.then()
        }
    }

    private suspend fun generateJwtToken(claims: Map<String, Any>): JwtResponseDto {
        return jwtTokenGenerator.build(claims)
    }

    private suspend fun ServerHttpResponse.sendJwtResponseAsRedirect(
        jwtResponse: JwtResponseDto,
        redirectUrl: String?,
        redirectType: RedirectType,
    ) {
//        val isJson = ReactorContextHolder.isMobileDevice() || redirectType == RedirectType.JSON
        redirectUrl?.let {
//            if (isJson) {
//                this.sendJwtResponseAsJson(jwtResponse)
//            } else {
//                this.sendJwtResponseAsCookie(jwtResponse)
            this.setRedirectConfigurationWithQueryParams(redirectUrl, jwtResponse)
//            this.setRedirectConfiguration(redirectUrl)
//            }
        } ?: this.sendJwtResponseAsJson(jwtResponse)
    }

    private suspend fun ServerHttpResponse.setRedirectConfigurationWithQueryParams(
        redirectUrl: String,
        jwtResponse: JwtResponseDto,
    ) {
        val uri = URI.create(redirectUrl)
        val queryParams = uri.query?.let { "$it&" } ?: ""
        val updatedUrl = URI.create(
            "${uri.scheme}://${uri.authority}${uri.path}?${queryParams}accessToken=${jwtResponse.accessToken}&refreshToken=${jwtResponse.refreshToken}&accessTokenExpiresIn=${jwtResponse.accessTokenExpiresIn}&refreshTokenExpiresIn=${jwtResponse.refreshTokenExpiresIn}",
        )
        this.headers.location = updatedUrl
        this.statusCode = HttpStatus.FOUND
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

package com.example.auth.business.service.oauth

import com.example.auth.business.service.JwtTokenService
import com.example.auth.domain.model.application.RedirectType
import com.example.auth.domain.model.oauth.AbstractSocialUser
import com.example.auth.domain.repository.redis.RedisDriver
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
    private val jwtTokenService: JwtTokenService,
) : ServerAuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication?,
    ): Mono<Void> {
        return Mono.defer {
            val socialLoginUser =
                authentication?.principal as? AbstractSocialUser
                    ?: return@defer Mono.error<Void>(IllegalArgumentException("Authentication principal is not valid"))
            mono {
                val origin = webFilterExchange.exchange.request.headers.origin
                val response = webFilterExchange.exchange.response
                val jwtResponse = generateJwtTokenAndSaveToken(socialLoginUser.getClaims())
                response.sendJwtResponseAsRedirect(
                    jwtResponse = jwtResponse,
                    redirectUrl = socialLoginUser.redirectUrl,
                    redirectType = socialLoginUser.redirectType,
                )
            }.then()
        }
    }

    private suspend fun generateJwtTokenAndSaveToken(claims: Map<String, Any>): JwtResponseDto =
        jwtTokenService.buildAndSave(claims)

    private suspend fun ServerHttpResponse.sendJwtResponseAsRedirect(
        jwtResponse: JwtResponseDto,
        redirectUrl: String?,
        redirectType: RedirectType,
        origin: String?,
    ) {
//        val isJson = ReactorContextHolder.isMobileDevice() || redirectType == RedirectType.JSON
        redirectUrl?.let {
            when (redirectType) {
                RedirectType.REDIRECT_WITH_COOKIE -> this.sendJwtResponseAsCookie(jwtResponse, redirectUrl, origin)
                RedirectType.JSON -> this.sendJwtResponseAsJson(jwtResponse)
                RedirectType.QUERY_PARAM -> this.setRedirectConfigurationWithQueryParams(redirectUrl, jwtResponse)
            }
        } ?: this.sendJwtResponseAsJson(jwtResponse)
    }

    private suspend fun ServerHttpResponse.setRedirectConfigurationWithQueryParams(
        redirectUrl: String,
        jwtResponse: JwtResponseDto,
    ) {
        val uri = URI.create(redirectUrl)
        val queryParams = uri.query?.let { "$it&" } ?: ""
        val updatedUrl =
            URI.create(
                "${uri.scheme}://${uri.authority}${uri.path}?${queryParams}accessToken=${jwtResponse.accessToken}&refreshToken=${jwtResponse.refreshToken}&accessTokenExpiresIn=${jwtResponse.accessTokenExpiresIn}&refreshTokenExpiresIn=${jwtResponse.refreshTokenExpiresIn}",
            )
        this.headers.location = updatedUrl
        this.statusCode = HttpStatus.FOUND
    }

    private suspend fun ServerHttpResponse.sendJwtResponseAsCookie(jwtResponse: JwtResponseDto, redirectUrl: String, origin: String?) {
        this.apply {
            addCookie(createCookie("accessToken", jwtResponse.accessToken, jwtResponse.accessTokenExpiresIn))
            addCookie(createCookie("refreshToken", jwtResponse.refreshToken, jwtResponse.refreshTokenExpiresIn))
            headers.add("Access-Control-Allow-Credentials", "true")
            origin?.let {
                headers.add("Access-Control-Allow-Origin", it)
            }
        }
        val uri = URI.create(redirectUrl)
        this.headers.location = uri
        this.statusCode = HttpStatus.FOUND
    }

    private fun createCookie(
        name: String,
        value: String,
        maxAge: Long,
    ): ResponseCookie =
        ResponseCookie
            .from(name, value)
            .httpOnly(false)
            .secure(true)
            .path("/")
            .domain(".platformholder.site")
            .maxAge(Duration.ofMillis(maxAge))
            .sameSite("None")
            .build()

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

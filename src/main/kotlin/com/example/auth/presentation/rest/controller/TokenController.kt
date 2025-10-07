package com.example.auth.presentation.rest.controller

import annotation.AuthenticationUser
import com.example.auth.business.service.JwtTokenService
import com.example.auth.domain.repository.redis.RedisDriver
import com.example.auth.presentation.rest.controller.request.ReissueTokenRequest
import dto.Passport
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.woo.http.SucceededApiResponseBody

@RestController
@RequestMapping("/api/v1/auth")
class TokenController (
    private val jwtTokenService: JwtTokenService,
){
    @PostMapping("/oauth/token")
    suspend fun getToken() {
        TODO()
    }

    @PostMapping("/oauth/revoke")
    suspend fun revokeToken(
        @AuthenticationUser
        @Parameter(hidden = true)
        passport: Passport,
        response: ServerHttpResponse,
    ): SucceededApiResponseBody<Unit> {
        jwtTokenService.revoke(response, passport)
        return SucceededApiResponseBody.succeed()
    }

    @PostMapping("/token/reissue")
    suspend fun reissueToken(
        @RequestBody
        request: ReissueTokenRequest,
    ): SucceededApiResponseBody<String> {
        val accessToken = jwtTokenService.reissueToken(request.refreshToken)
        return SucceededApiResponseBody(accessToken)
    }
}
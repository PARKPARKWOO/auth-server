package com.example.auth.business.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CookieService (
    @Value("\${jwt.access-token.expire-millis}")
    private val accessTokenExpireTime: Long,
    @Value("\${jwt.refresh-token.expire-millis}")
    private val refreshTokenExpireTime: Long,
){
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

    fun clearCookie(response: ServerHttpResponse) {
        response.addCookie(
            ResponseCookie
                .from("accessToken", "")
                .path("/")
                .maxAge(Duration.ZERO) // 삭제
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build(),
        )

        response.addCookie(
            ResponseCookie
                .from("refreshToken", "")
                .path("/")
                .maxAge(Duration.ZERO) // 삭제
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build(),
        )
    }
}
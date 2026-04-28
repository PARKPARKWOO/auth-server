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
            // P0-#2: JS 가 토큰을 직접 읽지 않도록 httpOnly. SameSite=None + secure 와 결합해
            // 크로스서브도메인 자격증명 송수신은 유지하되 XSS 노출 차단.
            .httpOnly(true)
            .secure(true)
            .path("/")
            .domain(".platformholder.site")
            .maxAge(Duration.ofMillis(maxAge))
            .sameSite("None")
            .build()

    fun clearCookie(response: ServerHttpResponse) {
        // LOGOUT-2: createCookie 와 (name, domain, path, secure, sameSite) 를 동일하게 맞춰야
        // 브라우저가 같은 쿠키로 인식하고 삭제. httpOnly 만 보안상 true 권장 (httpOnly 차이는
        // 브라우저 식별에 영향 없음).
        response.addCookie(buildClearedCookie("accessToken"))
        response.addCookie(buildClearedCookie("refreshToken"))
    }

    private fun buildClearedCookie(name: String): ResponseCookie =
        ResponseCookie
            .from(name, "")
            .path("/")
            .domain(".platformholder.site")
            .maxAge(Duration.ZERO)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .build()
}
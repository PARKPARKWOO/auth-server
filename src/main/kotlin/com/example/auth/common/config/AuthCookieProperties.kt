package com.example.auth.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("auth.cookie")
data class AuthCookieProperties(
    val accessTokenName: String = "accessToken",
    val refreshTokenName: String = "refreshToken",
    val domain: String?,
    val secure: Boolean,
    val sameSite: String,
    val path: String = "/",
    val httpOnly: Boolean = true,
)

package com.example.auth.presentation.rest.controller.request

import com.example.auth.domain.model.oauth.SocialProvider

data class OAuthTokenRequest(
    val provider: SocialProvider,
    val accessToken: String,
    val applicationId: String? = null, // 앱에서 전달받은 application ID (선택사항)
)


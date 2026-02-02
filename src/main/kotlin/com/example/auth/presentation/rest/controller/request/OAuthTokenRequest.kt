package com.example.auth.presentation.rest.controller.request

import com.example.auth.domain.model.oauth.SocialProvider

data class OAuthTokenRequest(
    val provider: SocialProvider,
    val accessToken: String,
    /** Application entity ID (String, e.g. UUID). provider + applicationId 로 ApplicationOAuthProvider 조회 */
    val applicationId: String? = null,
)


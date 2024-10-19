package com.example.auth.business.command

import com.example.auth.domain.model.oauth.SocialProvider

data class RegisterApplicationOAuthProviderCommand(
    val applicationId: String,
    val provider: SocialProvider,
    val redirectUri: String,
    val clientId: String,
    val clientSecret: String?,
)

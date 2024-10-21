package com.example.auth.presentation.request

import com.example.auth.business.command.RegisterApplicationOAuthProviderCommand
import com.example.auth.domain.model.oauth.SocialProvider

data class RegistrationApplicationOAuthRequest(
    val applicationId: String,
    val provider: SocialProvider,
    val clientId: String,
    val clientSecret: String?,
) {
    fun toCommand(): RegisterApplicationOAuthProviderCommand = RegisterApplicationOAuthProviderCommand(
        applicationId = applicationId,
        provider = provider,
        clientId = clientId,
        clientSecret = clientSecret,
    )
}

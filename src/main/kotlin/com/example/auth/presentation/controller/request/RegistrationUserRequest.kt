package com.example.auth.presentation.controller.request

import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.domain.model.oauth.SocialProvider

data class RegistrationUserRequest(
    val email: String?,
    val password: String = "",
    val socialId: String?,
    val provider: SocialProvider?,
    val name: String,
) {
    fun toCommand(): RegisterUserCommand =
        RegisterUserCommand(
            email = email,
            password = password,
            socialId = socialId,
            provider = provider,
            name = name,
        )
}

package com.example.auth.presentation.request

import com.example.auth.business.RegisterUserCommand
import com.example.auth.domain.model.SocialProvider

data class RegistrationUserRequest(
    val email: String?,
    val password: String = "",
    val socialId: String?,
    val provider: SocialProvider?,
) {
    fun toCommand(): RegisterUserCommand = RegisterUserCommand(
        email = email,
        password = password,
        socialId = socialId,
        provider = provider,
    )
}

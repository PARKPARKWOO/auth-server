package com.example.auth.business.command

import com.example.auth.domain.model.oauth.SocialProvider

data class RegisterUserCommand(
    val email: String?,
    val password: String,
    val socialId: String?,
    val provider: SocialProvider?,
)

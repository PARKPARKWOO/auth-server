package com.example.auth.business.command

import com.example.auth.domain.model.oauth.SocialLoginUser
import com.example.auth.domain.model.oauth.SocialProvider

data class RegisterUserCommand(
    val email: String?,
    val password: String,
    val socialId: String?,
    val provider: SocialProvider?,
    val name: String?,
) {
    companion object {
        fun from(socialLoginUser: SocialLoginUser) = RegisterUserCommand(
            email = socialLoginUser.email,
            password = "",
            socialId = socialLoginUser.getId(),
            provider = socialLoginUser.getProvider(),
            name = socialLoginUser.name,
        )
    }
}

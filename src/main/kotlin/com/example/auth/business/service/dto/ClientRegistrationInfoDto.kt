package com.example.auth.business.service.dto

import com.example.auth.domain.model.oauth.SocialProvider

data class ClientRegistrationInfoDto(
    val id: Long,
    val redirectUri: String,
    val applicationName: String,
    val clientSecret: String?,
    val clientId: String,
    val provider: SocialProvider,
)

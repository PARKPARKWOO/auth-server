package com.example.auth.domain.model.oauth

enum class SocialProvider(
    val clientNamePrefix: String,
) {
    KAKAO("Kakao for "),
    GOOGLE("Google for "),
}

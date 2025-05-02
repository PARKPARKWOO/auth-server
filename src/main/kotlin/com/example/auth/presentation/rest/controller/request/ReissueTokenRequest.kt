package com.example.auth.presentation.rest.controller.request

data class ReissueTokenRequest(
    val refreshToken: String,
)

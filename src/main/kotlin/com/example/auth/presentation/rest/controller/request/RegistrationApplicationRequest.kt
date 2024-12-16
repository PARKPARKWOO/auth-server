package com.example.auth.presentation.rest.controller.request

import com.example.auth.domain.model.application.RedirectType

data class RegistrationApplicationRequest(
    val name: String,
    val redirectUrl: String,
    val redirectType: RedirectType,
)

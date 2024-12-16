package com.example.auth.presentation.rest.controller.request

import com.example.auth.business.command.RegisterApplicationDomainCommand

data class RegistrationDomainRequest(
    val applicationId: String,
    val domains: List<String>,
) {
    fun toCommand(): RegisterApplicationDomainCommand =
        RegisterApplicationDomainCommand(
            applicationId = applicationId,
            domains = domains,
        )
}

package com.example.auth.business.command

data class RegisterApplicationDomainCommand(
    val applicationId: String,
    val domains: List<String>,
)

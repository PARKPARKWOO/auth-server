package com.example.auth.domain.repository

import com.example.auth.domain.model.application.ApplicationOAuthProvider
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface ApplicationOAuthProviderRepository : ReactiveCrudRepository<ApplicationOAuthProvider, Long>

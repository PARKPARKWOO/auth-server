package com.example.auth.domain.repository.application

import com.example.auth.domain.entity.application.ApplicationOAuthProvider
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface ApplicationOAuthProviderRepository : ReactiveCrudRepository<ApplicationOAuthProvider, Long>

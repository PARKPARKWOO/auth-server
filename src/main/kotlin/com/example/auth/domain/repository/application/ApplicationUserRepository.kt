package com.example.auth.domain.repository.application

import com.example.auth.domain.entity.applicationuser.ApplicationUser
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface ApplicationUserRepository : R2dbcRepository<ApplicationUser, Long> {
    fun findByApplicationIdAndUserId(applicationId: String, userId: String): Mono<ApplicationUser>
}
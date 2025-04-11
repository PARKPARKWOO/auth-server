package com.example.auth.domain.repository.application

import com.example.auth.domain.entity.application.ApplicationAuthority
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux

interface ApplicationAuthorityRepository : R2dbcRepository<ApplicationAuthority, Long> {
    fun findByApplicationIdOrderByLevelAsc(applicationId: String): Flux<ApplicationAuthority>
}
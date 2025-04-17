package com.example.auth.domain.repository.application

import com.example.auth.domain.entity.application.Application
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface ApplicationRepository : R2dbcRepository<Application, String> {
    fun findByName(name: String): Mono<Application?>
}

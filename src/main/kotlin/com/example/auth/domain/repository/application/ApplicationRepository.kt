package com.example.auth.domain.repository.application

import com.example.auth.domain.entity.application.Application
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface ApplicationRepository : R2dbcRepository<Application, String> {
    override fun <S : Application?> save(entity: S & Any): Mono<S> {
        return this.save(entity).doOnNext { it.markNotNew() }
    }
    fun findByName(name: String): Mono<Application?>
}

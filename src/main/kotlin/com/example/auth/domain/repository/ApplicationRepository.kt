package com.example.auth.domain.repository

import com.example.auth.domain.model.application.Application
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface ApplicationRepository : ReactiveCrudRepository<Application, String> {
    override fun <S : Application?> save(entity: S & Any): Mono<S> {
        return this.save(entity).doOnNext { it.markNotNew() }
    }
}

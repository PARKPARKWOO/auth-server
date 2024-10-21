package com.example.auth.business.service

import com.example.auth.domain.model.application.Application
import com.example.auth.domain.repository.ApplicationRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class ApplicationFinder(
    private val applicationRepository: ApplicationRepository,
) {
    suspend fun findByName(name: String): Application? = applicationRepository.findByName(name).awaitSingleOrNull()

    suspend fun findById(applicationId: String): Application? =
        applicationRepository.findById(applicationId).awaitSingleOrNull()
}

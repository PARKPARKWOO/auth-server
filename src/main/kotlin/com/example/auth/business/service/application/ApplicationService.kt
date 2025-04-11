package com.example.auth.business.service.application

import com.example.auth.domain.entity.application.Application
import com.example.auth.domain.entity.application.ApplicationAuthority
import com.example.auth.domain.entity.applicationuser.ApplicationUser
import com.example.auth.domain.repository.application.ApplicationAuthorityRepository
import com.example.auth.domain.repository.application.ApplicationRepository
import com.example.auth.domain.repository.application.ApplicationUserRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class ApplicationService(
    private val applicationRepository: ApplicationRepository,
    private val applicationAuthorityRepository: ApplicationAuthorityRepository,
    private val applicationUserRepository: ApplicationUserRepository,
) {
    suspend fun findByName(name: String): Application? = applicationRepository.findByName(name).awaitSingleOrNull()

    suspend fun findById(applicationId: String): Application? =
        applicationRepository.findById(applicationId).awaitSingleOrNull()

    suspend fun createAuthority(applicationId: String, authority: String, level: Int): ApplicationAuthority =
        coroutineScope {
            val authorityEntity =
                ApplicationAuthority.create(applicationId = applicationId, authority = authority, level = level)
            applicationAuthorityRepository.save(authorityEntity).awaitSingle()
        }

    suspend fun getApplicationAuthority(id: Long): ApplicationAuthority = coroutineScope {
        applicationAuthorityRepository.findById(id).awaitSingle()
    }

    suspend fun getApplicationAuthority(applicationId: String): ApplicationAuthority = coroutineScope {
        applicationAuthorityRepository.findByApplicationIdOrderByLevelAsc(applicationId).awaitFirst()
    }

    suspend fun getApplicationUser(applicationId: String, userId: String): ApplicationUser? = coroutineScope {
        applicationUserRepository.findByApplicationIdAndUserId(applicationId, userId)
            .awaitSingleOrNull()
    }

    suspend fun createApplicationUser(applicationId: String, userId: String, authorityId: Long): ApplicationUser =
        coroutineScope {
            val user = ApplicationUser.create(userId, applicationId, authorityId)
            applicationUserRepository.save(user).awaitSingle()
        }
}

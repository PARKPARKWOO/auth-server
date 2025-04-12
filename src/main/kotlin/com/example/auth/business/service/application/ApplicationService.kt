package com.example.auth.business.service.application

import com.example.auth.domain.entity.application.Application
import com.example.auth.domain.entity.application.ApplicationAuthority
import com.example.auth.domain.entity.applicationuser.ApplicationUser
import com.example.auth.domain.repository.application.ApplicationAuthorityRepository
import com.example.auth.domain.repository.application.ApplicationRepository
import com.example.auth.domain.repository.application.ApplicationUserRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

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

    suspend fun getLowLevelApplicationAuthority(applicationId: String): ApplicationAuthority = coroutineScope {
        applicationAuthorityRepository.findByApplicationIdOrderByLevelAsc(applicationId).awaitFirst()
    }

    suspend fun getApplicationAuthorityList(applicationId: String): List<ApplicationAuthority> =
        applicationAuthorityRepository.findAllByApplicationId(applicationId)
            .collectList()
            .awaitSingle()

    suspend fun getApplicationUser(applicationId: String, userId: String): ApplicationUser? = coroutineScope {
        applicationUserRepository.findByApplicationIdAndUserId(applicationId, userId)
            .awaitSingleOrNull()
    }

    suspend fun createApplicationUser(applicationId: String, userId: String, authorityId: Long): ApplicationUser =
        coroutineScope {
            val user = ApplicationUser.create(userId, applicationId, authorityId)
            applicationUserRepository.save(user).awaitSingle()
        }

    fun getApplicationUserByPage(pageable: Pageable, applicationId: String): Flux<ApplicationUser> =
        applicationUserRepository.findAllByApplicationId(pageable, applicationId)

    suspend fun getTotalApplicationUserCount(applicationId: String): Long =
        applicationUserRepository.countByApplicationId(applicationId).awaitSingle()
}

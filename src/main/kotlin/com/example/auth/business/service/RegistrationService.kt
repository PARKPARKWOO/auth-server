package com.example.auth.business.service

import com.example.auth.business.command.RegisterApplicationDomainCommand
import com.example.auth.business.command.RegisterApplicationOAuthProviderCommand
import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.domain.entity.application.Application
import com.example.auth.domain.entity.application.ApplicationAuthority
import com.example.auth.domain.entity.application.ApplicationDomain
import com.example.auth.domain.entity.application.ApplicationOAuthProvider
import com.example.auth.domain.entity.user.User
import com.example.auth.domain.model.application.RedirectType
import com.example.auth.domain.repository.application.ApplicationDomainRepository
import com.example.auth.domain.repository.application.ApplicationOAuthProviderRepository
import com.example.auth.domain.repository.application.ApplicationRepository
import com.example.auth.domain.repository.UserRepository
import com.example.auth.domain.repository.application.ApplicationAuthorityRepository
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class RegistrationService(
    private val userRepository: UserRepository,
    private val applicationRepository: ApplicationRepository,
    private val applicationOAuthProviderRepository: ApplicationOAuthProviderRepository,
    private val applicationDomainRepository: ApplicationDomainRepository,
    private val applicationAuthorityRepository: ApplicationAuthorityRepository,
    private val transactionalOperator: TransactionalOperator,
) {
    suspend fun registerUser(command: RegisterUserCommand): User {
        return transactionalOperator.execute {
            val user: User = User.fromCommand(command)
            userRepository.save(user)
        }.awaitSingle()
    }

    suspend fun registerApplication(
        name: String,
        redirectUrl: String,
        redirectType: RedirectType
    ): String {
        return transactionalOperator.execute {
            val application = Application.create(
                name = name,
                redirectUrl = redirectUrl,
                redirectType = redirectType.name
            )
            applicationRepository.save(application).flatMap { savedApplication ->
                val roleUser = ApplicationAuthority.createRoleUser(savedApplication.id)
                val roleAdmin = ApplicationAuthority.createRoleAdmin(savedApplication.id)
                Mono.zip(applicationAuthorityRepository.save(roleAdmin), applicationAuthorityRepository.save(roleUser))
                    .thenReturn(savedApplication.id)
            }
        }.awaitSingle()
    }

    suspend fun registerApplicationOAuthProvider(
        command: RegisterApplicationOAuthProviderCommand,
    ): ApplicationOAuthProvider {
        val applicationOAuthProvider = ApplicationOAuthProvider(
            applicationId = command.applicationId,
            provider = command.provider,
            clientId = command.clientId,
            clientSecret = command.clientSecret,
        )
        return applicationOAuthProviderRepository.save(applicationOAuthProvider).awaitSingle()
    }

    suspend fun registerApplicationDomainsForCors(
        command: RegisterApplicationDomainCommand,
    ) {
        val application = applicationRepository.findById(command.applicationId).awaitSingle()

        transactionalOperator.executeAndAwait {
            val applicationDomains = command.domains.map { domain ->
                ApplicationDomain(
                    applicationId = command.applicationId,
                    domain = domain,
                )
            }
            Flux.fromIterable(applicationDomains)
                .flatMap { applicationDomainRepository.save(it) }
                .collectList()
                .awaitSingle()
        }
    }
}

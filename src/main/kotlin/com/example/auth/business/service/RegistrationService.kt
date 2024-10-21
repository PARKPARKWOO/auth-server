package com.example.auth.business.service

import com.example.auth.business.command.RegisterApplicationDomainCommand
import com.example.auth.business.command.RegisterApplicationOAuthProviderCommand
import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.domain.model.application.Application
import com.example.auth.domain.model.application.ApplicationDomain
import com.example.auth.domain.model.application.ApplicationOAuthProvider
import com.example.auth.domain.model.user.User
import com.example.auth.domain.repository.ApplicationDomainRepository
import com.example.auth.domain.repository.ApplicationOAuthProviderRepository
import com.example.auth.domain.repository.ApplicationRepository
import com.example.auth.domain.repository.UserRepository
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import reactor.core.publisher.Flux

@Service
class RegistrationService(
    private val userRepository: UserRepository,
    private val applicationRepository: ApplicationRepository,
    private val applicationOAuthProviderRepository: ApplicationOAuthProviderRepository,
    private val applicationDomainRepository: ApplicationDomainRepository,
    private val transactionalOperator: TransactionalOperator,
) {
    suspend fun registerUser(command: RegisterUserCommand): String {
        return transactionalOperator.execute {
            val user: User = User.fromCommand(command)
            userRepository.save(user).thenReturn(user.id)
        }.awaitSingle()
    }

    suspend fun registerApplication(name: String, redirectUrl: String): String {
        return transactionalOperator.execute {
            val application = Application(
                name = name,
                redirectUrl = redirectUrl,
            )
            applicationRepository.save(application).thenReturn(application.id)
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

    suspend fun registerApplicationDomains(
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

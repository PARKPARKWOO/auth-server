package com.example.auth.business.service

import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.domain.model.User
import com.example.auth.domain.repository.UserRepository
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator

@Service
class RegistrationService(
    private val userRepository: UserRepository,
    private val transactionalOperator: TransactionalOperator,
) {
    suspend fun registerUser(command: RegisterUserCommand): String {
        return transactionalOperator.execute {
            val user: User = User.fromCommand(command)
            userRepository.save(user).thenReturn(user.id)
        }.awaitSingle()
    }
}

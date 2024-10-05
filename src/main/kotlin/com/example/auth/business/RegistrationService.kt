package com.example.auth.business

import com.example.auth.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator

@Service
class RegistrationService(
    private val userRepository: UserRepository,
    private val transactionalOperator: TransactionalOperator,
) {
    suspend fun signUpUser() {
        return transactionalOperator.execute {
        }
    }
}

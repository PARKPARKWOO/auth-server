package com.example.auth.business

import com.example.auth.domain.model.User
import com.example.auth.domain.repository.UserRepository
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator

@Service
class RegistrationService(
    private val userRepository: UserRepository,
    private val transactionalOperator: TransactionalOperator,
    private val db: DatabaseClient,
) {
    suspend fun signUpUser(): Boolean {
        return transactionalOperator.execute {
            val user = User(
                email = null,
                password = "",
                socialId = null,
                provider = null,
                profile = null,
            )
            userRepository.save(user).thenReturn(true)
        }.awaitSingle()
    }
}

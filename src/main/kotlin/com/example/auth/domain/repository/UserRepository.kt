package com.example.auth.domain.repository

import com.example.auth.domain.model.user.User
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono
import java.util.UUID

interface UserRepository : ReactiveCrudRepository<User, UUID>, CustomUserRepository {
    override fun <S : User?> save(entity: S & Any): Mono<S> {
        return this.save(entity).doOnNext { it.markNotNew() }
    }
}

interface CustomUserRepository {
    suspend fun findBySocialIdAndProvider(socialId: String, provider: String): User?
}

class CustomUserRepositoryImpl(
    private val databaseClient: DatabaseClient,
) : CustomUserRepository {
    companion object {
        // column
        const val SOCIAL_ID_COLUMN = "social_id"
        const val PROVIDER_COLUMN = "provider"
        const val PASSWORD_COLUMN = "password"
        const val EMAIL_COLUMN = "email"

        // bind value
        const val SOCIAL_ID_BIND = "socialId"
        const val PROVIDER_BIND = "provider"
        const val PASSWORD_BIND = "password"
        const val EMAIL_BIND = "email"
    }

    override suspend fun findBySocialIdAndProvider(socialId: String, provider: String): User? {
        return databaseClient.sql("SELECT * FROM user WHERE $SOCIAL_ID_COLUMN = :$SOCIAL_ID_BIND AND $PROVIDER_COLUMN = :$PROVIDER_BIND")
            .bind(SOCIAL_ID_BIND, socialId)
            .bind(PROVIDER_BIND, provider)
            .map { row ->
                User.fromRow(row)
            }
            .one()
            .awaitSingleOrNull()
    }
}

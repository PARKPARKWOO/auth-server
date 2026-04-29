package com.example.auth.domain.repository

import com.example.auth.domain.entity.user.User
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitRowsUpdated
import reactor.core.publisher.Mono

interface UserRepository :
    ReactiveCrudRepository<User, String>,
    CustomUserRepository {
    override fun <S : User?> save(entity: S & Any): Mono<S> = this.save(entity).doOnNext { it.markNotNew() }
}

interface CustomUserRepository {
    suspend fun findBySocialIdAndProvider(
        socialId: String,
        provider: String,
    ): User?

    suspend fun findByEmailAndProvider(
        provider: String,
        email: String,
    ): User?

    suspend fun update(
        id: String,
        email: String?,
        name: String?,
    )
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
        const val ROLE_COLUMN = "role"
        const val NAME_COLUMN = "name"
        const val USER_ID_COLUMN = "id"

        // bind value
        const val SOCIAL_ID_BIND = "socialId"
        const val PROVIDER_BIND = "provider"
        const val PASSWORD_BIND = "password"
        const val ROLE_BIND = "role"
        const val EMAIL_BIND = "bind_email"
        const val NAME_BIND = "bind_name"
        const val USER_ID_BIND = "bind_id"
    }

    override suspend fun findBySocialIdAndProvider(
        socialId: String,
        provider: String,
    ): User? =
        databaseClient
            .sql("SELECT * FROM user WHERE $SOCIAL_ID_COLUMN = :$SOCIAL_ID_BIND AND $PROVIDER_COLUMN = :$PROVIDER_BIND")
            .bind(SOCIAL_ID_BIND, socialId)
            .bind(PROVIDER_BIND, provider)
            .map { row ->
                User.fromRow(row)
            }.one()
            .awaitSingleOrNull()

    override suspend fun findByEmailAndProvider(
        provider: String,
        email: String,
    ): User? =
        databaseClient
            .sql("SELECT * FROM user WHERE $EMAIL_COLUMN = :$EMAIL_BIND AND $PROVIDER_COLUMN = :$PROVIDER_BIND")
            .bind(EMAIL_BIND, email)
            .bind(PROVIDER_BIND, provider)
            .map { row ->
                User.fromRow(row)
            }.one()
            .awaitSingleOrNull()

    override suspend fun update(
        id: String,
        email: String?,
        name: String?,
    ) {
        // 핫픽스: 이전엔 SQL 은 email/name null 여부로 conditional 빌드되는데 binding 은 무조건 `name!!` 으로
        // 호출되어 name=null 인 경우 NPE → OAuth callback 흐름의 endUserWriter.update 가 500. 두 필드 모두
        // 변경 없으면 update 자체를 건너뛰고, 있는 필드만 binding 한다.
        if (email == null && name == null) return

        val sql = StringBuffer("UPDATE user set ")
        if (email != null) {
            sql.append("`$EMAIL_COLUMN` = :$EMAIL_BIND, ")
        }
        if (name != null) {
            sql.append("`$NAME_COLUMN` = :$NAME_BIND, ")
        }
        if (sql.endsWith(", ")) {
            sql.setLength(sql.length - 2)
        }
        sql.append(" WHERE `$USER_ID_COLUMN` = :$USER_ID_BIND")

        var spec = databaseClient.sql(sql.toString())
        if (email != null) spec = spec.bind(EMAIL_BIND, email)
        if (name != null) spec = spec.bind(NAME_BIND, name)
        spec.bind(USER_ID_BIND, id)
            .fetch()
            .awaitRowsUpdated()
    }
}

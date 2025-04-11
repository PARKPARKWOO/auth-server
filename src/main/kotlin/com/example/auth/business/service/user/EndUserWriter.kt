package com.example.auth.business.service.user

import com.example.auth.domain.entity.outbox.Outbox
import com.example.auth.domain.entity.user.User
import com.example.auth.domain.model.event.UserNameCapture
import com.example.auth.domain.model.oauth.GoogleUser
import com.example.auth.domain.model.oauth.KakaoUser
import com.example.auth.domain.model.oauth.NaverUser
import com.example.auth.domain.model.oauth.SocialLoginUser
import com.example.auth.domain.model.outbox.EventType
import com.example.auth.domain.model.outbox.RecordOperation
import com.example.auth.domain.repository.OutboxRepository
import com.example.auth.domain.repository.UserRepository
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.woo.mapper.Jackson

@Service
class EndUserWriter(
    private val transactionalOperator: TransactionalOperator,
    private val userRepository: UserRepository,
    private val outboxRepository: OutboxRepository,
) {
    suspend fun update(
        userEntity: User,
        socialUser: SocialLoginUser,
    ) {
        when (socialUser) {
            is KakaoUser -> {
                execute(userEntity, socialUser.email, socialUser.getNickname())
            }

            is GoogleUser -> {
                execute(userEntity, socialUser.email, socialUser.getNickname())
            }

            is NaverUser -> {
                execute(userEntity, socialUser.email, socialUser.getNickname())
            }

            else -> {
                throw IllegalArgumentException("Unsupported social user type: ${socialUser::class.simpleName}")
            }
        }
    }

    private suspend fun execute(
        userEntity: User,
        email: String?,
        name: String?,
    ) {
        val updateEmail = userEntity.updateEmail(email)
        val updateName = userEntity.updateName(name)
        val needUpdate = updateName != null || updateEmail != null
        if (needUpdate) {
            transactionalOperator.executeAndAwait {
                userRepository.update(
                    id = userEntity.id,
                    email = updateEmail,
                    name = updateName,
                )
                updateName?.let {
                    saveOutbox(userId = userEntity.id, name = updateName)
                }
            }
        }
    }

    private suspend fun saveOutbox(userId: String, name: String) {
        val payload = UserNameCapture(userId = userId, name = name)
        val outbox = Outbox.create(
            payload = Jackson.writeValueAsString(payload),
            eventType = EventType.UPDATE_USER_NAME,
            recordOperation = RecordOperation.UPDATE,
        )
        outboxRepository.save(outbox).awaitSingle()
    }
}

package com.example.auth.business.service

import com.example.auth.domain.entity.user.User
import com.example.auth.domain.model.oauth.GoogleUser
import com.example.auth.domain.model.oauth.KakaoUser
import com.example.auth.domain.model.oauth.SocialLoginUser
import com.example.auth.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait

@Service
class EndUserWriter(
    private val transactionalOperator: TransactionalOperator,
    private val userRepository: UserRepository,
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
            }
        }
    }
}

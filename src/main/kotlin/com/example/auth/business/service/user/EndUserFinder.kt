package com.example.auth.business.service.user

import com.example.auth.domain.entity.user.User
import com.example.auth.domain.model.oauth.SocialProvider
import com.example.auth.domain.repository.UserRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service

@Service
class EndUserFinder(
    private val userRepository: UserRepository,
) {
    suspend fun findBySocialIdAndProvider(
        socialId: String,
        provider: SocialProvider,
    ) = userRepository.findBySocialIdAndProvider(socialId = socialId, provider = provider.name)

    suspend fun findByEmailAndProvider(
        provider: SocialProvider,
        email: String,
    ) = userRepository.findByEmailAndProvider(provider = provider.name, email = email)

    suspend fun findById(id: String): User? = userRepository.findById(id).awaitSingleOrNull()
}

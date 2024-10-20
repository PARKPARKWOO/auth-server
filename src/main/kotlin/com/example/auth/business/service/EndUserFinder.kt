package com.example.auth.business.service

import com.example.auth.domain.model.oauth.SocialProvider
import com.example.auth.domain.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class EndUserFinder(
    private val userRepository: UserRepository,
) {
    suspend fun findBySocialIdAndProvider(socialId: String, provider: SocialProvider) =
        userRepository.findBySocialIdAndProvider(socialId = socialId, provider = provider.name)
}

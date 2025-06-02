package com.example.auth.presentation.grpc

import com.example.auth.business.service.JwtTokenService
import com.example.auth.domain.repository.redis.RedisDriver
import dto.JwtResponseDto
import io.grpc.Status
import io.grpc.StatusException
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.auth.grpc.TokenProto
import org.woo.auth.grpc.TokenServiceGrpcKt

@GrpcService
class TokenController(
    private val jwtTokenService: JwtTokenService,
    private val redisDriver: RedisDriver,
): TokenServiceGrpcKt.TokenServiceCoroutineImplBase() {
    companion object {
        const val REISSUE_TOKEN_LOCK_WAIT_TIME = 50L
        const val REISSUE_TOKEN_LOCK_LEASE_TIME = 3000L
        const val TOKEN_CACHE_TTL = 300L // 5 min
    }

    override suspend fun reissueToken(request: TokenProto.ReissueTokenRequest): TokenProto.JwtTokenResponse {
        return redisDriver.useLockOrNull(request.idempotentKey, REISSUE_TOKEN_LOCK_WAIT_TIME, REISSUE_TOKEN_LOCK_LEASE_TIME) {
            redisDriver.getValue(request.idempotentKey, JwtResponseDto::class.java)?.let {
                return@useLockOrNull it.toProto()
            }
            jwtTokenService.rotationToken(request.refreshToken).also {
                redisDriver.setValue(request.idempotentKey, it, it.refreshTokenExpiresIn)
            }.toProto()
        } ?: redisDriver.getValue(request.idempotentKey, JwtResponseDto::class.java)?.toProto() ?: throw StatusException(Status.UNAVAILABLE)
    }

    private fun JwtResponseDto.toProto(): TokenProto.JwtTokenResponse =
        TokenProto.JwtTokenResponse.newBuilder()
            .setAccessToken(this.accessToken)
            .setRefreshToken(this.refreshToken)
            .setAccessTokenExpiresIn(this.accessTokenExpiresIn)
            .setRefreshTokenExpiresIn(this.refreshTokenExpiresIn)
            .build()
}
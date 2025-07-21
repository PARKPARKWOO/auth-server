package com.example.auth.presentation.grpc

import com.example.auth.business.service.JwtTokenService
import com.example.auth.domain.repository.redis.RedisDriver
import dto.JwtResponseDto
import io.grpc.Status
import io.grpc.StatusException
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.apm.log.log
import org.woo.auth.grpc.TokenProto
import org.woo.auth.grpc.TokenServiceGrpcKt
import kotlin.math.log

@GrpcService
class TokenGrpcController(
    private val jwtTokenService: JwtTokenService,
    private val redisDriver: RedisDriver,
): TokenServiceGrpcKt.TokenServiceCoroutineImplBase() {
    companion object {
        const val REISSUE_TOKEN_LOCK_WAIT_TIME = 50L
        const val REISSUE_TOKEN_LOCK_LEASE_TIME = 3000L
        const val TOKEN_CACHE_TTL = 300L // 5 min
    }

    override suspend fun reissueToken(request: TokenProto.ReissueTokenRequest): TokenProto.JwtTokenResponse {
        log().info("incoming reissue-token")
        // TODO: refreshToken 예외처리 필요함
        val refreshTokenString = jwtTokenService.getUserIdFromRefreshToken(request.refreshToken).toString()
        return redisDriver.useLockOrNull(refreshTokenString, REISSUE_TOKEN_LOCK_WAIT_TIME, REISSUE_TOKEN_LOCK_LEASE_TIME) {
            log().info("use lock")
            redisDriver.getValue(refreshTokenString, JwtResponseDto::class.java)?.let {
                log().info("getValue")
                return@useLockOrNull it.toProto()
            }
            jwtTokenService.rotationToken(request.refreshToken).also {
                log().info("rotationToken")
                redisDriver.setValue(refreshTokenString, it, TOKEN_CACHE_TTL)
            }.toProto()
        } ?: redisDriver.getValue(refreshTokenString, JwtResponseDto::class.java)?.toProto()
        ?: throw Status.UNAVAILABLE.withDescription("Failed to acquire retry lock").asRuntimeException()
    }

    private fun JwtResponseDto.toProto(): TokenProto.JwtTokenResponse =
        TokenProto.JwtTokenResponse.newBuilder()
            .setAccessToken(this.accessToken)
            .setRefreshToken(this.refreshToken)
            .setAccessTokenExpiresIn(this.accessTokenExpiresIn)
            .setRefreshTokenExpiresIn(this.refreshTokenExpiresIn)
            .build()
}
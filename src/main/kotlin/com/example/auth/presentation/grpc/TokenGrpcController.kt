package com.example.auth.presentation.grpc

import com.example.auth.business.service.JwtTokenService
import com.example.auth.domain.repository.redis.RedisDriver
import com.google.protobuf.Empty
import dto.JwtResponseDto
import io.grpc.Context
import io.grpc.Status
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.apm.log.log
import org.woo.auth.grpc.TokenProto
import org.woo.auth.grpc.TokenServiceGrpcKt
import org.woo.grpc.AuthMetadata.JWT_TOKEN_CONTEXT_KEY

@GrpcService
class TokenGrpcController(
    private val jwtTokenService: JwtTokenService,
    private val redisDriver: RedisDriver,
): TokenServiceGrpcKt.TokenServiceCoroutineImplBase() {
    companion object {
        const val REISSUE_TOKEN_LOCK_WAIT_TIME = 50L
        const val REISSUE_TOKEN_LOCK_LEASE_TIME = 3000L
        const val TOKEN_CACHE_TTL = 300L // 5 min
        const val ROTATION_IDEMPOTENT_LOCK_PREFIX = "LOCK:IDEMPOTENT:"
        const val ROTATION_IDEMPOTENT_KEY_PREFIX = "IDEMPOTENT:RTR:"
    }

    override suspend fun reissueToken(request: TokenProto.ReissueTokenRequest): TokenProto.JwtTokenResponse {
        log().info("incoming reissue-token")
        // TODO: refreshToken 예외처리 필요함
        val refreshTokenString = jwtTokenService.getUserIdFromRefreshToken(request.refreshToken).toString()
        val lockKey = ROTATION_IDEMPOTENT_LOCK_PREFIX + refreshTokenString
        val valueKey = ROTATION_IDEMPOTENT_KEY_PREFIX + refreshTokenString
        return redisDriver.useLockOrNull(lockKey, REISSUE_TOKEN_LOCK_WAIT_TIME, REISSUE_TOKEN_LOCK_LEASE_TIME) {
            log().info("use lock")
            redisDriver.getValue(valueKey, JwtResponseDto::class.java)?.let {
                log().info("getValue")
                return@useLockOrNull it.toProto()
            }
            jwtTokenService.rotationToken(request.refreshToken).also {
                log().info("rotationToken")
                redisDriver.setValue(valueKey, it, TOKEN_CACHE_TTL)
            }.toProto()
        } ?: redisDriver.getValue(valueKey, JwtResponseDto::class.java)?.toProto()
        ?: throw Status.UNAVAILABLE.withDescription("Failed to acquire retry lock").asRuntimeException()
    }

    override suspend fun revokeToken(request: Empty): Empty {
        val accessToken = JWT_TOKEN_CONTEXT_KEY.get(Context.current())
        jwtTokenService.revoke(accessToken)
        return Empty.getDefaultInstance()
    }

    private fun JwtResponseDto.toProto(): TokenProto.JwtTokenResponse =
        TokenProto.JwtTokenResponse.newBuilder()
            .setAccessToken(this.accessToken)
            .setRefreshToken(this.refreshToken)
            .setAccessTokenExpiresIn(this.accessTokenExpiresIn)
            .setRefreshTokenExpiresIn(this.refreshTokenExpiresIn)
            .build()
}
package com.example.auth.business.service

import com.example.auth.domain.repository.redis.RedisDriver
import constant.AuthConstant
import dto.JwtResponseDto
import dto.Passport
import exception.ErrorCode
import exception.MalFormedTokenException
import exception.NoBearerTokenException
import exception.ParseJwtFailedException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Header
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Service
import org.woo.apm.log.log
import org.woo.auth.grpc.TokenProto.JwtTokenResponse
import java.util.Date
import java.util.UUID
import kotlin.math.exp
import exception.ErrorCode as AuthErrorCode

@Service
class JwtTokenService(
    @Value("\${jwt.access-token.secret-key}")
    private val accessTokenSecretKeyString: String,
    @Value("\${jwt.refresh-token.secret-key}")
    private val refreshTokenSecretKeyString: String,
    @Value("\${jwt.access-token.expire-millis}")
    private val accessTokenExpireTime: Long,
    @Value("\${jwt.refresh-token.expire-millis}")
    private val refreshTokenExpireTime: Long,
    private val redisDriver: RedisDriver,
    private val cookieService: CookieService,
) {
    companion object {
        const val REFRESH_TOKEN_REDIS_KEY_PREFIX = "refresh_token:"
        private const val USER_ID = "user_id:"
        private const val APPLICATION = "application:"
    }

    private val accessTokenSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessTokenSecretKeyString))
    private val refreshTokenSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshTokenSecretKeyString))

    suspend fun buildAndSave(claims: Map<String, Any>): JwtResponseDto {
        val jwtResponse = build(claims)
        val key = getRefreshTokenKey(
            userId = claims.getValue(AuthConstant.USER_ID).toString(),
            applicationId = claims.getValue(AuthConstant.APPLICATION_ID).toString()
        )
        redisDriver.setValue(
            key,
            jwtResponse.refreshToken,
            jwtResponse.refreshTokenExpiresIn
        )
        return jwtResponse
    }

    suspend fun reissueToken(refreshToken: String): String {
        val claims = parseRefreshToken(refreshToken)
        val userId = claims[AuthConstant.USER_ID].toString()
        val key = REFRESH_TOKEN_REDIS_KEY_PREFIX + userId
        return redisDriver.getValue(key, String::class.java)?.let { refreshTokenInRedis ->
            if (refreshTokenInRedis != refreshToken) throw MalFormedTokenException(AuthErrorCode.EXPIRED_JWT, null)
            buildAccessToken(claims)
        } ?: throw exception.ExpiredJwtException(errorCode = AuthErrorCode.EXPIRED_JWT, null)
    }

    suspend fun build(claims: Map<String, Any>): JwtResponseDto {
        val accessToken = buildAccessToken(claims)
        val refreshToken = buildRefreshToken(claims)
        return JwtResponseDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresIn = accessTokenExpireTime,
            refreshTokenExpiresIn = refreshTokenExpireTime,
        )
    }

    suspend fun buildAccessToken(claims: Map<String, Any>): String {
        val now = System.currentTimeMillis()
        return Jwts
            .builder()
            .setHeader(Jwts.header().setType(Header.JWT_TYPE))
            .setClaims(claims)
            .setExpiration(Date((now + accessTokenExpireTime)))
            .setIssuedAt(Date(now))
            .signWith(
                accessTokenSecretKey,
//                SignatureAlgorithm.ES512,
                SignatureAlgorithm.HS512,
            ).compact()
    }

    suspend fun buildRefreshToken(claims: Map<String, Any>): String {
        val now = System.currentTimeMillis()
        return Jwts
            .builder()
            .setHeader(Jwts.header().setType(Header.JWT_TYPE))
            .setClaims(claims)
            .setExpiration(Date((now + refreshTokenExpireTime)))
            .setIssuedAt(Date(now))
            .signWith(
                refreshTokenSecretKey,
                SignatureAlgorithm.HS512,
            ).compact()
    }

    suspend fun rotationToken(refreshToken: String): JwtResponseDto {
        val claims = parseRefreshToken(refreshToken)
        val userId = claims[AuthConstant.USER_ID].toString()
        val applicationId = claims[AuthConstant.APPLICATION_ID].toString()
        val key = getRefreshTokenKey(userId, applicationId)
        log().info("jwtTokenService.rotationToken")
        val refreshTokenInRedis = redisDriver.getValue(key, String::class.java)
        return refreshTokenInRedis?.let { token ->
            if (token != refreshToken) throw MalFormedTokenException(AuthErrorCode.EXPIRED_JWT, null)
            buildAndSave(claims)
        } ?: throw exception.ExpiredJwtException(errorCode = AuthErrorCode.EXPIRED_JWT, null)
    }

    fun parseAccessToken(token: String?): Map<String, Any> =
        try {
            val removeBearerToken =
                token?.removeBearer() ?: throw NoBearerTokenException(ErrorCode.NO_BEARER_TOKEN, null)
            Jwts
                .parserBuilder()
                .setSigningKey(accessTokenSecretKey)
                .build()
                .parseClaimsJws(removeBearerToken)
                .body
        } catch (e: ExpiredJwtException) {
            throw exception.ExpiredJwtException(AuthErrorCode.EXPIRED_JWT, e)
        } catch (e: JwtException) {
            log().error("access token parsing failed")
            throw ParseJwtFailedException(AuthErrorCode.PARSE_JWT_FAILED, e)
        }

    suspend fun parseRefreshToken(refreshToken: String): Map<String, Any> =
        try {
            val removeBearerToken = refreshToken.removeBearer()
            Jwts
                .parserBuilder()
                .setSigningKey(refreshTokenSecretKey)
                .build()
                .parseClaimsJws(removeBearerToken)
                .body
        } catch (e: ExpiredJwtException) {
            throw exception.ExpiredJwtException(AuthErrorCode.EXPIRED_JWT, e)
        } catch (e: JwtException) {
            throw ParseJwtFailedException(AuthErrorCode.PARSE_JWT_FAILED, e)
        }

    suspend fun getUserIdFromRefreshToken(refreshToken: String): UUID =
        UUID.fromString(parseRefreshToken(refreshToken)[AuthConstant.USER_ID].toString())

    fun getUserIdFromAccessToken(accessToken: String?): UUID =
        UUID.fromString(parseAccessToken(accessToken)[AuthConstant.USER_ID].toString())

    fun getSignInApplicationIdFromAccessToken(accessToken: String): String =
        parseAccessToken(accessToken)[AuthConstant.APPLICATION_ID].toString()

    fun getRoleFromAccessToken(accessToken: String?): String =
        parseAccessToken(accessToken)[AuthConstant.USER_ROLE].toString()


    private fun String.removeBearer(): String {
        if (this.startsWith(AuthConstant.BEARER_PREFIX)) return this.removePrefix(AuthConstant.BEARER_PREFIX)
        return this
    }

    private fun getRefreshTokenKey(userId: String, applicationId: String) =
        REFRESH_TOKEN_REDIS_KEY_PREFIX + userId + USER_ID + APPLICATION + applicationId

    suspend fun revoke(response: ServerHttpResponse, passport: Passport) {
        val key = getRefreshTokenKey(passport.userId.toString(), passport.signInApplicationId)
        cookieService.clearJwtCookies(response)
        redisDriver.delete(key).awaitSingle()
    }
    suspend fun revoke(accessToken: String) {
        val userId = getUserIdFromAccessToken(accessToken)
        val applicationId = getSignInApplicationIdFromAccessToken(accessToken)
        val key = getRefreshTokenKey(userId.toString(), applicationId)
        redisDriver.delete(key).awaitSingle()
    }
}

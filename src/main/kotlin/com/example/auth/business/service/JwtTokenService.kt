package com.example.auth.business.service

import com.example.auth.business.exception.MalFormedTokenException
import com.example.auth.business.exception.ParseJwtFailedException
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.domain.repository.redis.RedisDriver
import constant.AuthConstant
import dto.JwtResponseDto
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Header
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.woo.apm.log.log
import java.util.Date
import java.util.UUID
import com.example.auth.business.exception.ExpiredJwtException as CustomExpiredJwtException

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
) {
    private val accessTokenSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessTokenSecretKeyString))
    private val refreshTokenSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshTokenSecretKeyString))

    suspend fun buildAndSave(claims: Map<String, Any>): JwtResponseDto {
        val jwtResponse = build(claims)
        redisDriver.setValue(
            claims.getValue(AuthConstant.USER_ID).toString(),
            jwtResponse.refreshToken,
            jwtResponse.refreshTokenExpiresIn
        )
        return jwtResponse
    }

    suspend fun reissueToken(refreshToken: String): String {
        val claims = parseRefreshToken(refreshToken)
        val userId = claims[AuthConstant.USER_ID].toString()
        return redisDriver.getValue(userId, String::class.java)?.let { refreshTokenInRedis ->
            if (refreshTokenInRedis != refreshToken) throw MalFormedTokenException(ErrorCode.EXPIRED_JWT, null)
            buildAccessToken(claims)
        } ?: throw CustomExpiredJwtException(ErrorCode.EXPIRED_JWT, null)
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

    fun parseAccessToken(token: String): Map<String, Any> =
        try {
            val removeBearerToken = token.removeBearer()
            Jwts
                .parserBuilder()
                .setSigningKey(accessTokenSecretKey)
                .build()
                .parseClaimsJws(removeBearerToken)
                .body
        } catch (e: ExpiredJwtException) {
            throw CustomExpiredJwtException(ErrorCode.EXPIRED_JWT, e)
        } catch (e: JwtException) {
            log().error("accessToken parse error from $token")
            throw ParseJwtFailedException(ErrorCode.PARSE_JWT_FAILED, e)
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
            throw CustomExpiredJwtException(ErrorCode.EXPIRED_JWT, e)
        } catch (e: JwtException) {
            throw ParseJwtFailedException(ErrorCode.PARSE_JWT_FAILED, e)
        }

    suspend fun getUserIdFromRefreshToken(refreshToken: String): UUID =
        UUID.fromString(parseRefreshToken(refreshToken)[AuthConstant.USER_ID].toString())

    fun getUserIdFromAccessToken(accessToken: String): UUID =
        UUID.fromString(parseAccessToken(accessToken)[AuthConstant.USER_ID].toString())

    fun getSignInApplicationIdFromAccessToken(accessToken: String): String =
        parseAccessToken(accessToken)[AuthConstant.APPLICATION_ID].toString()

    companion object {
        fun minKeyStringLength(algorithm: SignatureAlgorithm) = algorithm.minKeyLength.let { (it + 5) / 6 }
    }

    private fun String.removeBearer(): String {
        if (this.startsWith(AuthConstant.BEARER_PREFIX)) return this.removePrefix(AuthConstant.BEARER_PREFIX)
        return this
    }
}

package com.example.auth.business.service

import com.example.auth.business.exception.ParseJwtFailedException
import com.example.auth.common.http.error.ErrorCode
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
) {
    private val accessTokenSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessTokenSecretKeyString))
    private val refreshTokenSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshTokenSecretKeyString))

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
            Jwts
                .parserBuilder()
                .setSigningKey(refreshTokenSecretKey)
                .build()
                .parseClaimsJws(refreshToken)
                .body
        } catch (e: ExpiredJwtException) {
            throw CustomExpiredJwtException(ErrorCode.EXPIRED_JWT, e)
        } catch (e: JwtException) {
            throw ParseJwtFailedException(ErrorCode.PARSE_JWT_FAILED, e)
        }

    suspend fun getUserIdFromRefreshToken(refreshToken: String): UUID =
        UUID.fromString(parseRefreshToken(refreshToken)[AuthConstant.USER_ID].toString())

    fun getUserIdFromAccessTokenToken(accessToken: String): UUID =
        UUID.fromString(parseAccessToken(accessToken)[AuthConstant.USER_ID].toString())

    fun getSignInApplicationIdFromAccessTokenToken(accessToken: String): String =
        parseAccessToken(accessToken)[AuthConstant.APPLICATION_ID].toString()

    companion object {
        fun minKeyStringLength(algorithm: SignatureAlgorithm) = algorithm.minKeyLength.let { (it + 5) / 6 }
    }

    private fun String.removeBearer(): String {
        if (this.startsWith(AuthConstant.BEARER_PREFIX)) return this.removePrefix(AuthConstant.BEARER_PREFIX)
        return this
    }
}

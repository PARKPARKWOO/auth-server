package com.example.auth.common.http.error

import com.example.auth.common.http.response.FailedApiResponseBody
import org.springframework.boot.logging.LogLevel
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatusCode

enum class ErrorCode(
    val message: String,
    val httpCode: HttpStatusCode,
    val level: LogLevel,
) {
    NO_BEARER_TOKEN("", HttpStatus.UNAUTHORIZED, LogLevel.WARN),
    EXPIRED_JWT("", HttpStatus.UNAUTHORIZED, LogLevel.WARN),
    PARSE_JWT_FAILED("", BAD_REQUEST, LogLevel.WARN),
    REISSUE_JWT_TOKEN_FAILURE("", HttpStatus.UNAUTHORIZED, LogLevel.WARN),

    FORBIDDEN("작업을 수행할 권한이 없습니다.", HttpStatus.FORBIDDEN, LogLevel.WARN),

    AUTHENTICATION_RESOLVER_ERROR("", HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR),
    NOT_FOUND_REQUEST("", BAD_REQUEST, LogLevel.WARN),

    // oauth
    NOT_FOUND_REGISTRATION("registrationId 가 존재 하지 않습니다", BAD_REQUEST, LogLevel.WARN),

    // user
    NOT_FOUND_USER("user 를 찾을 수 없습니다", HttpStatus.BAD_REQUEST, LogLevel.WARN),

    UNKNOWN_ERROR("알 수 없는 에러", HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR),
}

fun ErrorCode.toFailedResponseBody(): FailedApiResponseBody =
    FailedApiResponseBody(
        code = this.name,
        message = this.message,
    )

package com.example.auth.common.http.error

import org.springframework.boot.logging.LogLevel
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatusCode
import org.woo.grpc.ErrorConverter
import org.woo.http.FailedApiResponseBody

enum class ErrorCode(
    val message: String,
    val httpCode: HttpStatusCode,
    val level: LogLevel,
) {
    FORBIDDEN("작업을 수행할 권한이 없습니다.", HttpStatus.FORBIDDEN, LogLevel.WARN),
    AUTHENTICATION_RESOLVER_ERROR("", HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR),
    NOT_FOUND_REQUEST("", BAD_REQUEST, LogLevel.WARN),

    // oauth
    NOT_FOUND_REGISTRATION("registrationId 가 존재 하지 않습니다", BAD_REQUEST, LogLevel.WARN),

    // user
    NOT_FOUND_USER("user 를 찾을 수 없습니다", HttpStatus.BAD_REQUEST, LogLevel.WARN),

    // application
    NOT_FOUNT_APPLICATION("등록되어 있는 application 이 없습니다", BAD_REQUEST, LogLevel.WARN),

    UNKNOWN_ERROR("알 수 없는 에러", HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR),
}

fun ErrorCode.toFailedResponseBody(): FailedApiResponseBody =
    FailedApiResponseBody(
        code = this.name,
        message = this.message,
    )

fun ErrorCode.toGrpcError() = ErrorConverter.toGrpcErrorResponse(message = this.message, status = this.httpCode.value())

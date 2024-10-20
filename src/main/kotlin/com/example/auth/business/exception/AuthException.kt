package com.example.auth.business.exception

import com.example.auth.common.http.error.ErrorCode

open class AuthException(open val errorCode: ErrorCode, override val cause: Throwable?) :
    RuntimeException(errorCode.message, cause)

data class ExpiredJwtException(override val errorCode: ErrorCode, override val cause: Throwable) :
    AuthException(errorCode = errorCode, cause = cause)

data class ParseJwtFailedException(override val errorCode: ErrorCode, override val cause: Throwable) :
    AuthException(errorCode = errorCode, cause = cause)

data class NoBearerTokenException(override val errorCode: ErrorCode, override val cause: Throwable?) :
    AuthException(errorCode = errorCode, cause = cause)

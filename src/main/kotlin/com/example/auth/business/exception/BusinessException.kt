package com.example.auth.business.exception

import com.example.auth.common.http.error.ErrorCode

open class BusinessException(open val errorCode: ErrorCode, override val cause: Throwable?) :
    RuntimeException(errorCode.message, cause)

data class NotFoundRegistrationException(override val errorCode: ErrorCode, override val cause: Throwable?) :
    BusinessException(errorCode = errorCode, cause = cause)

package com.example.auth.presentation.rest.exception

import com.example.auth.business.exception.BusinessException
import exception.AuthException
import org.springframework.boot.logging.LogLevel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.woo.apm.log.log
import org.woo.http.FailedApiResponseBody
import exception.LogLevel as AuthLogLevel

@RestControllerAdvice
class ExceptionHandler {
    @ExceptionHandler(AuthException::class)
    fun handleAuthException(e: AuthException): ResponseEntity<FailedApiResponseBody> {
        logging(e)
        val body = FailedApiResponseBody(e.errorCode.name, e.errorCode.message)
        return ResponseEntity.status(e.errorCode.httpCode).body(body)
    }

    private fun logging(e: Exception) {
        when (e) {
            is AuthException -> {
                handleAuthExceptionLogging(e.errorCode.level)
            }

            is BusinessException -> {
                handleBusinessExceptionLogging(e.errorCode.level)
            }

            else -> {
                log().error("authentication request failed")
            }
        }
    }

    private fun handleBusinessExceptionLogging(level: LogLevel) {
        when (level) {
            LogLevel.DEBUG -> log().debug("authentication business request rejected")
            LogLevel.INFO -> log().info("authentication business request rejected")
            LogLevel.WARN -> log().warn("authentication business request rejected")
            LogLevel.ERROR -> log().error("authentication business request rejected")
            LogLevel.TRACE -> log().trace("authentication business request rejected")
            LogLevel.OFF -> log().info("authentication business request rejected")
            else -> log().info("authentication business request rejected")
        }
    }

    private fun handleAuthExceptionLogging(level: AuthLogLevel) {
        when (level) {
            AuthLogLevel.DEBUG -> log().debug("authentication request rejected")
            AuthLogLevel.INFO -> log().info("authentication request rejected")
            AuthLogLevel.WARN -> log().warn("authentication request rejected")
            AuthLogLevel.ERROR -> log().error("authentication request rejected")
            AuthLogLevel.TRACE -> log().trace("authentication request rejected")
            else -> log().info("authentication request rejected")
        }
    }
}

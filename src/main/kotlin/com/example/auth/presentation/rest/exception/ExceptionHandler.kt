package com.example.auth.presentation.rest.exception

import com.example.auth.business.exception.BusinessException
import exception.AuthException
import exception.ErrorCode
import org.springframework.boot.logging.LogLevel
import org.springframework.http.HttpRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebExchange
import org.woo.apm.log.log
import org.woo.http.FailedApiResponseBody
import exception.LogLevel as AuthLogLevel

@RestControllerAdvice
class ExceptionHandler {
    @ExceptionHandler(AuthException::class)
    fun handleAuthException(e: AuthException, exchange: ServerWebExchange): ResponseEntity<FailedApiResponseBody> {
        logging(e, exchange.request.uri.path)
        val body = FailedApiResponseBody(e.errorCode.name, e.errorCode.message)
        return ResponseEntity.status(e.errorCode.httpCode).body(body)
    }

    private fun logging(e: Exception, path: String) {
        when (e) {
            is AuthException -> {
                handleAuthExceptionLogging(e, e.errorCode.level, path)
            }

            is BusinessException -> {
                handleExceptionLogging(e, e.errorCode.level, path)
            }

            else -> {
                handleExceptionLogging(e, LogLevel.ERROR, path)
            }
        }
    }

    private fun handleExceptionLogging(e: Exception, level: LogLevel, path: String) {
        val message: String = logMessage(null, path, e)
        when (level) {
            LogLevel.DEBUG -> log().debug(message)
            LogLevel.INFO -> log().info(message)
            LogLevel.WARN -> log().warn(message)
            LogLevel.ERROR -> log().error(message)
            LogLevel.TRACE -> log().trace(message)
            LogLevel.OFF -> log().info(message)
            else -> log().info(message)
        }
    }

    private fun handleAuthExceptionLogging(e: AuthException, level: AuthLogLevel, path: String) {
        val message = logMessage(e.errorCode, path, e)
        when (level) {
            AuthLogLevel.DEBUG -> log().debug(message)
            AuthLogLevel.INFO -> log().info(message)
            AuthLogLevel.WARN -> log().warn(message)
            AuthLogLevel.ERROR -> log().error(message)
            AuthLogLevel.TRACE -> log().trace(message)
            else -> log().info(message)
        }
    }

    private fun logMessage(errorCode: ErrorCode?, path: String, e: Exception): String =
        """
            errorCode = ${errorCode?.name}
            message = ${errorCode?.message}
            requestPath = $path
            stackTrace = ${e.printStackTrace()}
            cause = ${e.cause}
            message = ${e.message}
        """.trimIndent()
}
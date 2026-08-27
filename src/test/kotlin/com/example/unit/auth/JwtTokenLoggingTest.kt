package com.example.unit.auth

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.example.auth.business.service.CookieService
import com.example.auth.business.service.JwtTokenService
import com.example.auth.domain.repository.redis.RedisDriver
import com.example.auth.presentation.grpc.interceptor.JwtTokenInterceptor
import constant.AuthConstant
import io.grpc.Metadata
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import model.Role
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.woo.grpc.AuthMetadata.AUTHORIZATION_METADATA_KEY
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertTrue

class JwtTokenLoggingTest {
    @Test
    fun `rotation lookup and parse failure never render raw token values`() =
        runTest {
            val redisDriver = mockk<RedisDriver>()
            val service = tokenService(redisDriver)
            val claims =
                mapOf<String, Any>(
                    AuthConstant.USER_ID to "00000000-0000-4000-8000-000000000001",
                    AuthConstant.USER_ROLE to Role.ROLE_USER.name,
                    AuthConstant.APPLICATION_ID to "mirror-view",
                )
            val validRefreshToken = service.build(claims).refreshToken
            val invalidAccessToken = "invalid-access-token-sentinel"
            coEvery { redisDriver.getValue(any(), String::class.java) } returns validRefreshToken
            coEvery { redisDriver.setValue(any(), any<String>(), any()) } returns Unit

            captureLogs(JwtTokenService::class.java) { appender ->
                service.rotationToken(validRefreshToken)
                assertFails { service.parseAccessToken(invalidAccessToken) }

                val rendered = appender.list.joinToString("\n") { it.formattedMessage }
                assertTrue(appender.list.isNotEmpty(), "expected the JwtTokenService logger to be exercised")
                assertFalse(rendered.contains(validRefreshToken))
                assertFalse(rendered.contains(invalidAccessToken))
            }
        }

    @Test
    fun `interceptor never renders inbound bearer`() =
        runTest {
            val inboundBearer = "Bearer inbound-bearer-token-sentinel"
            val metadata = Metadata().apply { put(AUTHORIZATION_METADATA_KEY, inboundBearer) }

            captureLogs(JwtTokenInterceptor::class.java) { appender ->
                JwtTokenInterceptor().extractJwtToken(metadata)

                val rendered = appender.list.joinToString("\n") { it.formattedMessage }
                assertFalse(rendered.contains(inboundBearer))
                assertFalse(rendered.contains(inboundBearer.removePrefix("Bearer ")))
            }
        }

    private fun tokenService(redisDriver: RedisDriver) =
        JwtTokenService(
            accessTokenSecretKeyString = TEST_SECRET,
            refreshTokenSecretKeyString = TEST_SECRET,
            accessTokenExpireTime = 60_000,
            refreshTokenExpireTime = 120_000,
            redisDriver = redisDriver,
            cookieService = mockk<CookieService>(relaxed = true),
        )

    private suspend fun <T : Any> captureLogs(
        loggerType: Class<T>,
        action: suspend (ListAppender<ILoggingEvent>) -> Unit,
    ) {
        val logger = LoggerFactory.getLogger(loggerType) as Logger
        val simpleNameLogger = LoggerFactory.getLogger(loggerType.simpleName) as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start() }
        logger.addAppender(appender)
        simpleNameLogger.addAppender(appender)
        try {
            action(appender)
        } finally {
            logger.detachAppender(appender)
            simpleNameLogger.detachAppender(appender)
            appender.stop()
        }
    }

    private companion object {
        const val TEST_SECRET =
            "cXdxd3F3cXdxd3F3cXdxd3F3cXdxd3F3cXdxd3F3cXdxd3F3cXdxd3F3cXdxd3F3cXdxd3F3cXdxd3F3cXdxd3F3cXdxdw=="
    }
}

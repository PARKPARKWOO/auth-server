package com.example.unit.grpc

import com.example.auth.business.service.JwtTokenService
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.business.service.user.EndUserFinder
import com.example.auth.presentation.grpc.UserInfoController
import com.example.util.GrpcTestUtil.TEST_CONTEXT
import com.google.protobuf.Empty
import exception.ErrorCode
import exception.ExpiredJwtException
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UserInfoControllerTest {
    private val endUserFinder = mockk<EndUserFinder>()
    private val jwtTokenService = mockk<JwtTokenService>()
    private val applicationService = mockk<ApplicationService>()
    private val controller = UserInfoController(endUserFinder, jwtTokenService, applicationService)

    @Test
    fun `expired passport response exposes only stable error marker without throwable cause`() = runTest {
        every { jwtTokenService.getUserIdFromAccessToken(any()) } throws
            ExpiredJwtException(ErrorCode.EXPIRED_JWT, IllegalStateException("raw-token-detail"))

        val error =
            assertFailsWith<StatusRuntimeException> {
                TEST_CONTEXT.call {
                    runBlocking { controller.getPassportByBearer(Empty.getDefaultInstance()) }
                }
            }

        assertEquals(Status.Code.UNAUTHENTICATED, error.status.code)
        assertEquals(ErrorCode.EXPIRED_JWT.name, error.status.description)
        assertNull(error.cause)
    }
}

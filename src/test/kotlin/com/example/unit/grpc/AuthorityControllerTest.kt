package com.example.unit.grpc

import com.example.auth.business.exception.BusinessException
import com.example.auth.business.service.JwtTokenService
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.presentation.grpc.AuthorityController
import com.example.util.ApplicationTestUtil.ADMIN_AUTHORITY
import com.example.util.ApplicationTestUtil.APPLICATION_ID
import com.example.util.ApplicationTestUtil.createAuthorityRequest
import com.example.util.ApplicationTestUtil.createUpdateRoleCommand
import com.example.util.GrpcTestUtil
import com.example.util.GrpcTestUtil.TEST_CONTEXT
import com.example.util.UserTestUtil.ADMIN_USER
import com.example.util.UserTestUtil.USER_ID
import com.google.protobuf.Empty
import io.grpc.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.woo.auth.grpc.AuthorityProto
import org.woo.grpc.AuthMetadata.JWT_TOKEN_CONTEXT_KEY
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthorityControllerTest {
    val applicationService: ApplicationService = mockk()
    val jwtService: JwtTokenService = mockk()
    val authorityController = AuthorityController(
        applicationService = applicationService,
        jwtTokenService = jwtService,
    )

    @Test
    fun `when create application authority then return Empty`() = runTest {
        val request = createAuthorityRequest("ROME_USER", APPLICATION_ID, 1)
        every { jwtService.getUserIdFromAccessToken(any()) } returns USER_ID
        every { jwtService.getSignInApplicationIdFromAccessToken(any()) } returns APPLICATION_ID
        coEvery { applicationService.getApplicationAuthority(any()) } returns ADMIN_AUTHORITY
        coEvery { applicationService.getApplicationUser(any(), any()) } returns ADMIN_USER
        coEvery { applicationService.createAuthority(any(), any(), any()) } returns ADMIN_AUTHORITY

        val result = TEST_CONTEXT.call {
            runBlocking { authorityController.createApplicationAuthority(request) }
        }
        assertEquals(Empty.getDefaultInstance(), result)
    }

    @Test
    fun `when update application user role then return Empty`() = runTest {
        // 테스트에 사용할 dummy JWT 토큰 값
        val dummyToken = "dummy-token"
        // gRPC Context에 토큰 주입
        val testContext = Context.current().withValue(JWT_TOKEN_CONTEXT_KEY, dummyToken)

        // 테스트용 UpdateApplicationUserRoleCommand 생성 (프로토콜 버퍼 빌더 사용)
        val request = createUpdateRoleCommand(
            APPLICATION_ID,
            1L,
            "target-user-id"

        )
        // jwtService 모킹 설정
        every { jwtService.getUserIdFromAccessToken(any()) } returns USER_ID
        every { jwtService.getSignInApplicationIdFromAccessToken(any()) } returns APPLICATION_ID

        // Admin 사용자 검증 관련 applicationService 모킹 설정
        coEvery { applicationService.getApplicationUser(any(), any()) } returns ADMIN_USER
        coEvery { applicationService.getApplicationAuthority(any()) } returns ADMIN_AUTHORITY

        // updateApplicationUserRole 호출에 대한 모킹 설정 (반환값 없이 Unit 반환)
        coEvery { applicationService.updateApplicationUserRole(any(), any(), any()) } returns Unit

        val result = GrpcTestUtil.TEST_CONTEXT.call {
            runBlocking { authorityController.updateApplicationUserRole(request) }
        }
        assertEquals(Empty.getDefaultInstance(), result)
    }


    @Test
    fun `when update application user role with mismatched application id then throw BusinessException FORBIDDEN`() = runTest {
        val request = AuthorityProto.UpdateApplicationUserRoleCommand.newBuilder()
            .setApplicationId("different-application-id")
            .setTargetUserId("target-user-id")
            .setAuthorityId(1L)
            .build()

        // jwtService 모킹: 토큰으로부터 정상적으로 값을 반환하도록 설정
        every { jwtService.getUserIdFromAccessToken(any()) } returns USER_ID
        every { jwtService.getSignInApplicationIdFromAccessToken(any()) } returns APPLICATION_ID

        // 관리자 검증을 위해 applicationService 모킹 설정
        coEvery { applicationService.getApplicationUser(any(), any()) } returns ADMIN_USER
        coEvery { applicationService.getApplicationAuthority(any()) } returns ADMIN_AUTHORITY

        // updateApplicationUserRole 호출 시, applicationId 불일치로 예외 발생 예상
        val exception = assertFailsWith<BusinessException> {
            GrpcTestUtil.TEST_CONTEXT.call {
                runBlocking { authorityController.updateApplicationUserRole(request) }
            }
        }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    /**
     * 테스트 케이스 2: verifyAdminUser에서 applicationService.getApplicationUser가 null을 반환할 때
     * BusinessException(NOT_FOUND_USER)이 발생해야 함
     */
    @Test
    fun `when update application user role with not found user then throw BusinessException NOT_FOUND_USER`() = runTest {
        // 요청의 applicationId는 토큰으로부터 얻은 값과 일치하도록 설정
        val request = AuthorityProto.UpdateApplicationUserRoleCommand.newBuilder()
            .setApplicationId(APPLICATION_ID)
            .setTargetUserId("target-user-id")
            .setAuthorityId(1L)
            .build()

        // jwtService 모킹
        every { jwtService.getUserIdFromAccessToken(any()) } returns USER_ID
        every { jwtService.getSignInApplicationIdFromAccessToken(any()) } returns APPLICATION_ID

        // 사용자 조회가 실패하도록 null 반환 (NOT_FOUND_USER 예외 발생)
        coEvery { applicationService.getApplicationUser(any(), any()) } returns null
        // userAuthority에 관련 모킹은 필요 없으므로 생략

        val exception = assertFailsWith<BusinessException> {
            GrpcTestUtil.TEST_CONTEXT.call {
                runBlocking { authorityController.updateApplicationUserRole(request) }
            }
        }

        assertEquals(ErrorCode.NOT_FOUND_USER, exception.errorCode)
    }
}
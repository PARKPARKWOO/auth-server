package com.example.auth.presentation.grpc

import com.example.auth.business.exception.BusinessException
import com.example.auth.business.service.JwtTokenService
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.common.http.error.ErrorCode
import com.google.protobuf.Empty
import io.grpc.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.auth.grpc.AuthorityProto
import org.woo.auth.grpc.AuthorityProto.AuthorityInfoResponse
import org.woo.auth.grpc.AuthorityServiceGrpcKt
import org.woo.grpc.AuthMetadata.JWT_TOKEN_CONTEXT_KEY
import java.util.*

@GrpcService
class AuthorityController(
    private val applicationService: ApplicationService,
    private val jwtTokenService: JwtTokenService,
) : AuthorityServiceGrpcKt.AuthorityServiceCoroutineImplBase() {
    override suspend fun createApplicationAuthority(request: AuthorityProto.CreateApplicationAuthorityRequest): Empty {
        val (applicationId, userId) = getApplicationAndUserId()
        verifyAdminUser(userId.toString(), applicationId)
        applicationService.createAuthority(
            applicationId = request.applicationId,
            authority = request.authority,
            level = request.level,
        )
        return Empty.getDefaultInstance()
    }

    override suspend fun updateApplicationUserRole(request: AuthorityProto.UpdateApplicationUserRoleCommand): Empty {
        val (applicationId, userId) = getApplicationAndUserId()
        verifyAdminUser(userId.toString(), applicationId)
        if (request.applicationId != applicationId) throw BusinessException(ErrorCode.FORBIDDEN, null)
        applicationService.updateApplicationUserRole(request.targetUserId, request.authorityId, applicationId)
        return Empty.getDefaultInstance()
    }

    private suspend fun verifyAdminUser(userId: String, applicationId: String) {
        val applicationUser = applicationService.getApplicationUser(applicationId, userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_USER, null)
        val userAuthority = applicationService.getApplicationAuthority(applicationUser.authorityId)
        if (userAuthority.level != Int.MAX_VALUE) throw BusinessException(ErrorCode.FORBIDDEN, null)
    }

    private fun getApplicationAndUserId(): Pair<String, UUID> {
        val token = JWT_TOKEN_CONTEXT_KEY.get(Context.current())
        val userId = jwtTokenService.getUserIdFromAccessToken(token)
        val applicationId = jwtTokenService.getSignInApplicationIdFromAccessToken(token)
        return Pair(applicationId, userId)
    }

    override fun getAuthorityInApplication(request: Empty): Flow<AuthorityProto.AuthorityInfoResponse> = flow {
        val (applicationId, userId) = getApplicationAndUserId()
        applicationService.getApplicationAuthorityList(applicationId).collect {
            val response = AuthorityInfoResponse.newBuilder()
                .setAuthority(it.authority)
                .setId(it.id)
                .setLevel(it.level)
                .build()
            emit(response)
        }
    }
}
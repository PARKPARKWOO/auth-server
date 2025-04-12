package com.example.auth.presentation.grpc

import com.example.auth.business.exception.BusinessException
import com.example.auth.business.service.JwtTokenService
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.common.http.error.ErrorCode
import com.google.protobuf.Empty
import io.grpc.Context
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.auth.grpc.AuthorityProto
import org.woo.auth.grpc.AuthorityServiceGrpcKt
import org.woo.grpc.AuthMetadata.JWT_TOKEN_CONTEXT_KEY

@GrpcService
class AuthorityController(
    private val applicationService: ApplicationService,
    private val jwtTokenService: JwtTokenService,
) : AuthorityServiceGrpcKt.AuthorityServiceCoroutineImplBase() {
    override suspend fun createApplicationAuthority(request: AuthorityProto.CreateApplicationAuthorityRequest): Empty {
        val token = JWT_TOKEN_CONTEXT_KEY.get(Context.current())
        val userId = jwtTokenService.getUserIdFromAccessTokenToken(token)
        val applicationId = jwtTokenService.getSignInApplicationIdFromAccessTokenToken(token)
        verifyUser(userId.toString(), applicationId)
        applicationService.createAuthority(
            applicationId = request.applicationId,
            authority = request.authority,
            level = request.level,
        )
        return Empty.getDefaultInstance()
    }

    private suspend fun verifyUser(userId: String, applicationId: String) {
        val applicationUser = applicationService.getApplicationUser(applicationId, userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_USER, null)
        val userAuthority = applicationService.getApplicationAuthority(applicationUser.authorityId)
        if (userAuthority.level != Int.MAX_VALUE) throw BusinessException(ErrorCode.FORBIDDEN, null)
    }
}
package com.example.auth.presentation.grpc

import com.example.auth.business.exception.NotFoundUserException
import com.example.auth.business.service.user.EndUserFinder
import com.example.auth.business.service.JwtTokenService
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.common.http.error.ErrorCode
import com.google.protobuf.Empty
import io.grpc.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.auth.grpc.AuthProto
import org.woo.auth.grpc.AuthProto.UserInfoResponse
import org.woo.auth.grpc.UserInfoServiceGrpcKt
import org.woo.grpc.AuthMetadata.JWT_TOKEN_CONTEXT_KEY

@GrpcService
class UserInfoController(
    private val endUserFinder: EndUserFinder,
    private val jwtTokenService: JwtTokenService,
    private val applicationService: ApplicationService,
) : UserInfoServiceGrpcKt.UserInfoServiceCoroutineImplBase() {
    override suspend fun getUserInfoByBearer(request: Empty): AuthProto.UserInfoResponse = coroutineScope {
//        return runCatching {
        val token = JWT_TOKEN_CONTEXT_KEY.get(Context.current())
        val userId = jwtTokenService.getUserIdFromAccessTokenToken(token)
        val applicationId = jwtTokenService.getSignInApplicationIdFromAccessTokenToken(token)
        val user = async {
            endUserFinder.findById(userId.toString())
                ?: throw NotFoundUserException(ErrorCode.NOT_FOUND_USER, null)
        }
        val authority =
            async {
                val applicationUser = applicationService.getApplicationUser(applicationId, userId.toString())
                    ?: throw NotFoundUserException(ErrorCode.NOT_FOUND_USER, null)
                applicationService.getApplicationAuthority(applicationUser.authorityId).authority
            }
//        }.onSuccess { user ->
        UserInfoResponse
            .newBuilder()
            .setId(user.await().id)
            .setEmail(user.await().email)
            .setName(user.await().name)
            .setRole(user.await().role)
            .setApplicationRole(authority.await())
            .setApplicationId(applicationId)
            .build()
//        }.onFailure {
//            log().error(it.stackTraceToString())
//            when (it) {
//                is AuthException -> {
//                    val error = it.errorCode.toGrpcError()
//                    val metadata = ErrorConverter.attachErrorToMetadata(error = error, data = null)
//                }
//                is BusinessException -> {
//                    val error = it.errorCode.toGrpcError()
//                    val metadata = ErrorConverter.attachErrorToMetadata(error = error, data = null)
//                    responseObserver
//                        ?.onError(INVALID_ARGUMENT.asRuntimeException(metadata))
//                }
//                else -> {}
//            }
//        }
    }
}

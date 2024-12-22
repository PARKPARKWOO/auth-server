package com.example.auth.presentation.grpc

import com.example.auth.business.exception.AuthException
import com.example.auth.business.exception.BusinessException
import com.example.auth.business.exception.NotFoundUserException
import com.example.auth.business.service.EndUserFinder
import com.example.auth.business.service.JwtTokenService
import com.example.auth.common.http.error.ErrorCode
import com.example.auth.common.http.error.toGrpcError
import com.google.protobuf.Empty
import io.grpc.Context
import io.grpc.Status.INVALID_ARGUMENT
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.auth.grpc.AuthProto
import org.woo.auth.grpc.UserInfoServiceGrpc
import org.woo.grpc.AuthMetadata.JWT_TOKEN_CONTEXT_KEY
import org.woo.grpc.ErrorConverter
import org.woo.log.log

@GrpcService
class UserInfoController(
    private val endUserFinder: EndUserFinder,
    private val jwtTokenService: JwtTokenService,
) : UserInfoServiceGrpc.UserInfoServiceImplBase() {
    override fun getUserInfoByBearer(
        empty: Empty?,
        responseObserver: StreamObserver<AuthProto.UserInfoResponse>?,
    ) {
        runCatching {
            val token = JWT_TOKEN_CONTEXT_KEY.get(Context.current())
            val userId = jwtTokenService.getUserIdFromAccessTokenToken(token)
            val user =
                runBlocking {
                    endUserFinder.findByUserId(userId.toString())
                } ?: throw NotFoundUserException(ErrorCode.NOT_FOUND_USER, null)
            AuthProto.UserInfoResponse
                .newBuilder()
                .setId(user.id)
                .setEmail(user.email)
                .setName(user.name)
                .setRole(user.role)
                .build()
        }.onSuccess { response ->
            // Send the response back to the client
            responseObserver?.onNext(response)
            responseObserver?.onCompleted()
        }.onFailure {
            log().error(it.stackTraceToString())
            when (it) {
                is AuthException -> {
                    val error = it.errorCode.toGrpcError()
                    val metadata = ErrorConverter.attachErrorToMetadata(error = error, data = null)
                    responseObserver
                        ?.onError(INVALID_ARGUMENT.asRuntimeException(metadata))
                }
                is BusinessException -> {
                    val error = it.errorCode.toGrpcError()
                    val metadata = ErrorConverter.attachErrorToMetadata(error = error, data = null)
                    responseObserver
                        ?.onError(INVALID_ARGUMENT.asRuntimeException(metadata))
                }
                else -> {}
            }
        }
    }
}

package com.example.auth.presentation.grpc

import com.example.auth.business.exception.NotFoundUserException
import com.example.auth.business.service.EndUserFinder
import com.example.auth.business.service.JwtTokenService
import com.example.auth.common.http.error.ErrorCode
import io.grpc.Context
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.auth.grpc.AuthProto
import org.woo.auth.grpc.UserInfoServiceGrpc
import org.woo.grpc.AuthMetadata.JWT_TOKEN_CONTEXT_KEY

@GrpcService
class UserInfoController(
    private val endUserFinder: EndUserFinder,
    private val jwtTokenService: JwtTokenService,
) : UserInfoServiceGrpc.UserInfoServiceImplBase() {
    override fun getUserInfoByBearer(
        request: AuthProto.UserInfoByBearerRequest?,
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
                .setRole(user.role)
                .build()
        }.onSuccess { response ->
            // Send the response back to the client
            responseObserver?.onNext(response)
            responseObserver?.onCompleted()
        }.onFailure {
            responseObserver?.onError(it)
            responseObserver?.onCompleted()
        }
    }
}

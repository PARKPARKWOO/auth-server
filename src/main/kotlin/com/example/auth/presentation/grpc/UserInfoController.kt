package com.example.auth.presentation.grpc

import com.example.auth.business.service.EndUserFinder
import com.example.auth.business.service.JwtTokenService
import com.example.auth.presentation.grpc.JwtTokenInterceptor.Companion.JWT_TOKEN_CONTEXT_KEY
import io.grpc.Context
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.auth.grpc.AuthProto
import org.woo.auth.grpc.UserInfoServiceGrpc

@GrpcService
class UserInfoController(
    private val endUserFinder: EndUserFinder,
    private val jwtTokenService: JwtTokenService,
) : UserInfoServiceGrpc.UserInfoServiceImplBase() {
    override fun getUserInfoByBearer(
        request: AuthProto.UserInfoByBearerRequest?,
        responseObserver: StreamObserver<AuthProto.UserInfoResponse>?,
    ) {
        val token = JWT_TOKEN_CONTEXT_KEY.get(Context.current())
        val userId = jwtTokenService.getUserIdFromAccessTokenToken(token)
        val user =
            runBlocking {
                endUserFinder.findByUserId(userId.toString())
            }
        if (user != null) {
            val response =
                AuthProto.UserInfoResponse
                    .newBuilder()
                    .setId(user.id)
                    .setEmail(user.email)
                    .setRole(user.role)
                    .build()

            // Send the response back to the client
            responseObserver?.onNext(response)
            responseObserver?.onCompleted()
        }
    }
}

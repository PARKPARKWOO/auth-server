package com.example.auth.presentation.grpc

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.springframework.stereotype.Component
import org.woo.grpc.AuthMetadata.AUTHORIZATION_METADATA_KEY
import org.woo.grpc.AuthMetadata.JWT_TOKEN_CONTEXT_KEY

@Component
@GrpcGlobalServerInterceptor
class JwtTokenInterceptor : ServerInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>?,
        headers: Metadata?,
        next: ServerCallHandler<ReqT, RespT>?,
    ): ServerCall.Listener<ReqT> {
        val jwtToken = headers?.get(AUTHORIZATION_METADATA_KEY)
        val context = Context.current().withValue(JWT_TOKEN_CONTEXT_KEY, jwtToken)
        return Contexts.interceptCall(context, call, headers, next)
    }
}

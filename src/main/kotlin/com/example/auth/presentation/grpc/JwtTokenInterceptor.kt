package com.example.auth.presentation.grpc

import com.example.auth.common.constants.AuthConstants.AUTHORIZATION_HEADER
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.springframework.stereotype.Component

@Component
@GrpcGlobalServerInterceptor
class JwtTokenInterceptor : ServerInterceptor {
    companion object {
        val AUTHORIZATION_METADATA_KEY: Metadata.Key<String> =
            Metadata.Key.of(AUTHORIZATION_HEADER, Metadata.ASCII_STRING_MARSHALLER)
        val JWT_TOKEN_CONTEXT_KEY: Context.Key<String> = Context.key("jwt_token")
    }

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

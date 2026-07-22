package com.example.auth.presentation.grpc.interceptor

import com.example.auth.business.exception.BusinessException
import exception.AuthException
import exception.ErrorCode
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.woo.apm.log.log

@GrpcGlobalServerInterceptor
class GrpcExceptionInterceptor: ServerInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata?,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
            next.startCall(call, headers)
        ) {

            override fun onMessage(message: ReqT) {
                try {
                    super.onMessage(message)
                } catch (e: AuthException) {
                    handleException(e)
                } catch (e: BusinessException) {
                    handleException(e)
                }
            }

            // 반쪽 연결 종료 시 예외 처리
            override fun onHalfClose() {
                try {
                    super.onHalfClose()
                } catch (e: AuthException) {
                    handleException(e)
                } catch (e: BusinessException) {
                    handleException(e)
                }
            }

            // 연결 취소 시 예외 처리
            override fun onCancel() {
                try {
                    super.onCancel()
                } catch (_: Exception) {
                    log().warn("gRPC cancellation handler failed")
                }
            }

            // 연결 완료 시 예외 처리
            override fun onComplete() {
                call.catchException {
                    super.onComplete()
                }
            }
        }
    }

    private fun <ReqT, ResT>ServerCall<ReqT, ResT>.catchException(block: () -> Unit) {
        try {
            block()
        } catch (e: AuthException) {
            handleException(e)
        } catch (e: BusinessException) {
            handleException(e)
        }
    }
    private fun handleException(@Suppress("UNUSED_PARAMETER") e: RuntimeException) {
        log().warn("gRPC request failed")
    }
}

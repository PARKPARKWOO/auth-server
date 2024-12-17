package com.example.auth.presentation.grpc

import com.example.auth.business.service.ApplicationOAuthService
import com.example.auth.business.service.dto.ClientRegistrationInfoDto
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.auth.grpc.GrpcTestServiceGrpc.GrpcTestServiceImplBase
import org.woo.auth.grpc.TestProto
import org.woo.auth.grpc.TestProto.TestData

@GrpcService
class TestGrpcService(
    private val applicationOAuthService: ApplicationOAuthService,
) : GrpcTestServiceImplBase() {
    override fun grpcTest(
        request: TestProto.GrpcTestRequest?,
        responseObserver: StreamObserver<TestProto.GrpcTestResponse>?,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // R2DBC 비동기 호출 및 데이터 처리
                val clientInfo = applicationOAuthService.findClientRegistrationInfoDto()

                // 응답 생성
                val response = buildGrpcResponse(clientInfo)

                // 성공 응답 전송
                responseObserver?.onNext(response)
                responseObserver?.onCompleted()
            } catch (e: Exception) {
                // 예외 발생 시 gRPC 에러 반환
                responseObserver?.onError(mapExceptionToGrpcStatus(e))
            }
        }
    }

    // gRPC 응답 빌드
    private fun buildGrpcResponse(clientInfoList: List<ClientRegistrationInfoDto>): TestProto.GrpcTestResponse {
        val builder = TestProto.GrpcTestResponse.newBuilder()
        clientInfoList.forEach { clientInfo ->
            builder.addData(
                TestData
                    .newBuilder()
                    .setClientId(clientInfo.clientId)
                    .setApplicationName(clientInfo.applicationName)
                    .setClientSecret(clientInfo.clientSecret ?: "")
                    .setProvider(clientInfo.provider.name)
                    .setId(clientInfo.id.toInt())
                    .build(),
            )
        }
        return builder.build()
    }

    // 예외를 gRPC Status로 변환
    private fun mapExceptionToGrpcStatus(e: Exception): StatusRuntimeException =
        when (e) {
            is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
            is NoSuchElementException -> Status.NOT_FOUND.withDescription(e.message).asRuntimeException()
            else ->
                Status.INTERNAL
                    .withDescription("Unexpected error occurred")
                    .withCause(e)
                    .asRuntimeException()
        }
}

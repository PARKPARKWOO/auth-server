package com.example.auth.presentation.grpc

import com.example.auth.business.service.application.ApplicationService
import com.google.protobuf.Empty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import net.devh.boot.grpc.server.service.GrpcService
import org.woo.auth.grpc.ApplicationProto
import org.woo.auth.grpc.ApplicationProto.ApplicationInfoResponse
import org.woo.auth.grpc.ApplicationServiceGrpcKt

@GrpcService
class ApplicationController(
    private val applicationService: ApplicationService,
): ApplicationServiceGrpcKt.ApplicationServiceCoroutineImplBase() {
    override fun getApplications(request: Empty): Flow<ApplicationInfoResponse> {
        return applicationService.getApplicationInfo().map {
            ApplicationInfoResponse.newBuilder()
                .setId(it.id)
                .setName(it.name)
                .build()
        }
    }
}
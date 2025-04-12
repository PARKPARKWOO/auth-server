package com.example.auth.presentation.grpc

import com.example.auth.business.exception.BusinessException
import com.example.auth.business.exception.NotFoundUserException
import com.example.auth.business.service.user.EndUserFinder
import com.example.auth.business.service.JwtTokenService
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.common.http.error.ErrorCode
import com.google.protobuf.Empty
import io.grpc.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.data.domain.PageRequest
import org.woo.auth.grpc.AuthProto
import org.woo.auth.grpc.AuthProto.UserInfoResponse
import org.woo.auth.grpc.UserInfoServiceGrpcKt
import org.woo.grpc.AuthMetadata.JWT_TOKEN_CONTEXT_KEY
import java.util.UUID

@GrpcService
class UserInfoController(
    private val endUserFinder: EndUserFinder,
    private val jwtTokenService: JwtTokenService,
    private val applicationService: ApplicationService,
) : UserInfoServiceGrpcKt.UserInfoServiceCoroutineImplBase() {
    override suspend fun getUserInfoByBearer(request: Empty): AuthProto.UserInfoResponse = coroutineScope {
//        return runCatching {
        val (applicationId, userId) = getApplicationAndUserId()
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

    override suspend fun getUserInfoInApplication(
        request: AuthProto.UserInfoInApplicationRequest
    ): AuthProto.UserInfoInApplicationPageResponse = coroutineScope {
        val (applicationId, userId) = getApplicationAndUserId()
        verifyUser(userId.toString(), applicationId)

        val pageIndex = if (request.page > 0) request.page - 1 else 0
        val size = if (request.size > 0) request.size else 20  // 기본값 20
        val pageRequest = PageRequest.of(pageIndex, size)

        val applicationUsersDeferred = async {
            applicationService.getApplicationUserByPage(pageRequest, applicationId)
                .collectList()
                .awaitSingle()
        }
        val authorityListDeferred = async {
            applicationService.getApplicationAuthorityList(applicationId)
        }
        val totalItemsDeferred = async {
            applicationService.getTotalApplicationUserCount(applicationId)
                .toInt()
        }

        val applicationUsers = applicationUsersDeferred.await()
        val authorityList = authorityListDeferred.await()
        val totalItems = totalItemsDeferred.await()
        val totalPages = if (size > 0) ((totalItems + size - 1) / size) else 1

        val userIds = applicationUsers.map { it.userId }
        val userDetails = endUserFinder.findAllByIds(userIds)
            .collectList()
            .awaitSingle()
        val userMap = userDetails.associateBy { it.id }

        val items = applicationUsers.mapNotNull { appUser ->
            userMap[appUser.userId]?.let { detail ->
                val authority = authorityList.find { it.id == appUser.authorityId }
                AuthProto.UserInfoInApplicationResponse.newBuilder()
                    .setUserId(appUser.userId)
                    .setUserName(detail.name)
                    .setEmail(detail.email)
                    .setApplicationRole(authority?.authority ?: "")
                    .build()
            }
        }
        AuthProto.UserInfoInApplicationPageResponse.newBuilder()
            .addAllItems(items)
            .setPage(pageIndex + 1)
            .setSize(size)
            .setTotalItems(totalItems)
            .setTotalPages(totalPages)
            .build()
    }


    private suspend fun getApplicationAndUserId(): Pair<String, UUID> {
        val token = JWT_TOKEN_CONTEXT_KEY.get(Context.current())
        val userId = jwtTokenService.getUserIdFromAccessTokenToken(token)
        val applicationId = jwtTokenService.getSignInApplicationIdFromAccessTokenToken(token)
        return Pair(applicationId, userId)
    }

    private suspend fun verifyUser(userId: String, applicationId: String) {
        val applicationUser = applicationService.getApplicationUser(applicationId, userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_USER, null)
        val userAuthority = applicationService.getApplicationAuthority(applicationUser.authorityId)
        if (userAuthority.level != Int.MAX_VALUE) throw BusinessException(ErrorCode.FORBIDDEN, null)
    }
}

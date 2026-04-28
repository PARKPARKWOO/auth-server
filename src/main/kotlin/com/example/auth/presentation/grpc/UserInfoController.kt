package com.example.auth.presentation.grpc

import com.example.auth.business.exception.BusinessException
import com.example.auth.business.exception.NotFoundUserException
import com.example.auth.business.service.user.EndUserFinder
import com.example.auth.business.service.JwtTokenService
import com.example.auth.business.service.application.ApplicationService
import com.example.auth.common.http.error.ErrorCode
import com.google.protobuf.Empty
import exception.ExpiredJwtException
import io.grpc.Context
import io.grpc.Status
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.data.domain.PageRequest
import org.woo.auth.grpc.AuthProto
import org.woo.auth.grpc.AuthProto.Passport
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
        val (applicationId, userId) = getApplicationAndUserId()
        buildUserInfo(applicationId, userId)
    }

    override suspend fun getPassportByBearer(request: Empty): AuthProto.Passport = coroutineScope {
        try {
            val token = JWT_TOKEN_CONTEXT_KEY.get(Context.current())
            val (applicationId, userId) = getApplicationAndUserId(token)
            val role = jwtTokenService.getRoleFromAccessToken(token)
            val userInfo = buildUserInfo(applicationId, userId)
            Passport.newBuilder()
                .setId(userId.toString())
                .setRole(role)
                .setApplicationId(applicationId)
                .setUserInfo(userInfo)
                .build()
        } catch (e: ExpiredJwtException) {
            throw Status.UNAUTHENTICATED
                .withDescription(e.message)
                .withCause(e)
                .asRuntimeException()
        }
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
        val authorityList = authorityListDeferred.await().toList()
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
        val userId = jwtTokenService.getUserIdFromAccessToken(token)
        val applicationId = jwtTokenService.getSignInApplicationIdFromAccessToken(token)
        return Pair(applicationId, userId)
    }

    private suspend fun getApplicationAndUserId(token: String): Pair<String, UUID> {
        val userId = jwtTokenService.getUserIdFromAccessToken(token)
        val applicationId = jwtTokenService.getSignInApplicationIdFromAccessToken(token)
        return Pair(applicationId, userId)
    }

    private suspend fun buildUserInfo(applicationId: String, userId: UUID): UserInfoResponse = coroutineScope {
        val userDeferred = async {
            endUserFinder.findById(userId.toString())
                ?: throw NotFoundUserException(ErrorCode.NOT_FOUND_USER, null)
        }
        val appRoleDeferred =
            async {
                val applicationUser = applicationService.getApplicationUser(applicationId, userId.toString())
                    ?: throw NotFoundUserException(ErrorCode.NOT_FOUND_USER, null)
                applicationService.getApplicationAuthority(applicationUser.authorityId)
            }
        val user = userDeferred.await()
        val appRole = appRoleDeferred.await()
        val responseBuilder = UserInfoResponse
            .newBuilder()
            .setApplicationRole(appRole.authority)
            .setAccessLevel(appRole.level)
        user.email?.let(responseBuilder::setEmail)
        user.name?.let(responseBuilder::setName)
        responseBuilder.build()
    }

    private suspend fun verifyUser(userId: String, applicationId: String) {
        val applicationUser = applicationService.getApplicationUser(applicationId, userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_USER, null)
        val userAuthority = applicationService.getApplicationAuthority(applicationUser.authorityId)
        if (userAuthority.level != Int.MAX_VALUE) throw BusinessException(ErrorCode.FORBIDDEN, null)
    }
}

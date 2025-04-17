package com.example.util

import com.example.auth.domain.entity.application.Application
import com.example.auth.domain.entity.application.ApplicationAuthority
import com.example.auth.domain.model.application.RedirectType
import org.woo.auth.grpc.AuthorityProto.CreateApplicationAuthorityRequest
import org.woo.auth.grpc.AuthorityProto.UpdateApplicationUserRoleCommand
import java.time.LocalDateTime

object ApplicationTestUtil {
    const val APPLICATION_ID: String = "id"
    const val APPLICATION_NAME = "name"
    val REDIRECT_TYPE = RedirectType.QUERY_PARAM.name
    const val REDIRECT_URL = "url"
    const val ROLE_ADMIN = "ROLE_ADMIN"
    const val ROLE_USER = "ROLE_USER"

    val CREATE_APPLICATION =
        Application.create(name = APPLICATION_NAME, redirectUrl = REDIRECT_URL, redirectType = REDIRECT_TYPE)
    val FIND_APPLICATION = Application(
        id = APPLICATION_ID,
        name = APPLICATION_NAME,
        redirectType = REDIRECT_TYPE,
        redirectUrl = REDIRECT_URL,
        createdAt = LocalDateTime.now(),
    )
    val ADMIN_AUTHORITY =
        ApplicationAuthority(applicationId = APPLICATION_ID, authority = ROLE_ADMIN, level = Int.MAX_VALUE)

    val USER_AUTHORITY =
        ApplicationAuthority(applicationId = APPLICATION_ID, authority = ROLE_USER, level = 0)

    fun createAuthorityRequest(
        authority: String,
        applicationId: String,
        level: Int,
    ): CreateApplicationAuthorityRequest = CreateApplicationAuthorityRequest
        .newBuilder()
        .setAuthority(authority)
        .setApplicationId(applicationId)
        .setLevel(level)
        .build()

    fun createUpdateRoleCommand(
        applicationId: String,
        authorityId: Long,
        targetId: String
    ): UpdateApplicationUserRoleCommand = UpdateApplicationUserRoleCommand.newBuilder()
        .setApplicationId(applicationId)
        .setAuthorityId(authorityId)
        .setTargetUserId(targetId)
        .build()
}
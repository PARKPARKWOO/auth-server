package com.example.util

import com.example.auth.domain.entity.applicationuser.ApplicationUser
import java.util.UUID

object UserTestUtil {
    val USER_ID = UUID.randomUUID()
    val ADMIN_USER = ApplicationUser(0L, USER_ID.toString(), ApplicationTestUtil.APPLICATION_ID, 0L)
}
package com.example.auth.domain.entity.user

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("user_oauth_token")
class UserOAuthToken(
    @Id
    @Column("user_id")
    val userId: String,
    @Column("access_token")
    val accessToken: String,
    @Column("refresh_token")
    val refreshToken: String?,

) {

}
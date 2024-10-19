package com.example.auth.domain.model.application

import com.example.auth.domain.model.oauth.SocialProvider
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(name = "application_oauth_provider")
class ApplicationOAuthProvider(
    @Id
    @Column("id")
    val id: Long = 0L,
    @Column("application_id")
    val applicationId: String,
    @Column("provider")
    val provider: SocialProvider,
    @Column("redirect_uri")
    var redirectUri: String,
    @Column("client_id")
    var clientId: String,
    @Column("client_secret")
    var clientSecret: String?,
)

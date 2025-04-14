package com.example.auth.domain.entity.application

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("application_authority")
class ApplicationAuthority(
    @Id
    @Column("id")
    val id: Long = 0L,
    @Column("application_id")
    val applicationId: String,
    @Column("authority")
    val authority: String,
    @Column("level")
    var level: Int,
) {
    companion object {
        fun create(applicationId: String, authority: String, level: Int): ApplicationAuthority = ApplicationAuthority(
            applicationId = applicationId,
            authority = authority,
            level = level,
        )

        fun createRoleUser(applicationId: String): ApplicationAuthority = ApplicationAuthority(
            applicationId = applicationId,
            authority = "ROLE_USER",
            level = 0
        )

        fun createRoleAdmin(applicationId: String): ApplicationAuthority = ApplicationAuthority(
            applicationId = applicationId,
            authority = "ROLE_ADMIN",
            level = Int.MAX_VALUE
        )
    }
}
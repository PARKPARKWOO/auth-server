package com.example.auth.domain.entity.applicationuser

import com.example.auth.domain.entity.R2dbcEntity
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("application_user")
class ApplicationUser(
    @Id
    val id: Long,
    @Column("user_id")
    val userId: String,
    @Column("application_id")
    val applicationId: String,
    @Column("authority_id")
    val authorityId: Long,
): R2dbcEntity<Long>() {
    companion object {
        fun create(userId: String, applicationId: String, authorityId: Long): ApplicationUser = ApplicationUser(
            id = 0L,
            applicationId = applicationId,
            userId = userId,
            authorityId = authorityId,
        )
    }
    @Column("created_at")
    @CreatedDate
    lateinit var createdAt: LocalDateTime
    override fun getId(): Long {
        return id
    }
}
package com.example.auth.domain.entity.applicationuser

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("application_user")
class ApplicationUser(
    @Id
    val id: Long,
    val userId: String,
    val applicationId: String,
    val role: String,
) {
    @Column("created_at")
    @CreatedDate
    lateinit var createdAt: LocalDateTime
}
package com.example.auth.domain.entity.organization

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("organization_application")
class OrganizationApplication(
    @Id
    val id: Long,
    @Column("organization_id")
    val organizationId: String,
    @Column("application_id")
    val applicationId: String,
) {
    @Column("created_at")
    @CreatedDate
    lateinit var createdAt: LocalDateTime
}
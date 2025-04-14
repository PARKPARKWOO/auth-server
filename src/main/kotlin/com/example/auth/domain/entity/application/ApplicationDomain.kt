package com.example.auth.domain.entity.application

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("application_domain")
class ApplicationDomain(
    @Id
    @Column("id")
    val id: Long = 0L,
    @Column("application_id")
    val applicationId: String,
    @Column("domain")
    val domain: String,
)

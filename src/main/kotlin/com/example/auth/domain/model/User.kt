package com.example.auth.domain.model

import com.fasterxml.uuid.Generators
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("user")
class User(
    @Id
    val id: UUID = Generators.timeBasedEpochGenerator().generate(),
    @Column("email")
    val email: String?,
    @Column("password")
    val password: String,
    @Column("social_id")
    val socialId: String?,
    @Column("provider")
    val provider: String?,
    @Column("profile")
    val profile: String?,
) {
    @Column("created_at")
    @CreatedDate
    lateinit var createdAt: LocalDateTime

    @Column("updated_at")
    @LastModifiedDate
    lateinit var updatedAt: LocalDateTime

    @Column
    var deletedAt: LocalDateTime? = null
}

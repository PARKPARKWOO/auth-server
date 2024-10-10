package com.example.auth.domain.model.user

import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.domain.model.oauth.SocialProvider
import com.fasterxml.uuid.Generators
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("user")
class User(
    @Column("id")
    val id: String = Generators.timeBasedEpochGenerator().generate().toString(),
    @Column("email")
    val email: String?,
    @Column("password")
    val password: String,
    @Column("social_id")
    val socialId: String?,
    @Column("provider")
    val provider: SocialProvider?,
) {
    @Column("created_at")
    @CreatedDate
    lateinit var createdAt: LocalDateTime

    @Column("updated_at")
    @LastModifiedDate
    lateinit var updatedAt: LocalDateTime

    @Column("deleted_at")
    var deletedAt: LocalDateTime? = null

    companion object {
        fun fromCommand(command: RegisterUserCommand): User = User(
            email = command.email,
            password = command.password,
            socialId = command.socialId,
            provider = command.provider,
        )
    }
}

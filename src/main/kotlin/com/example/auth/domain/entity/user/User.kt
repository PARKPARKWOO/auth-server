package com.example.auth.domain.entity.user

import com.example.auth.business.command.RegisterUserCommand
import com.example.auth.domain.repository.CustomUserRepositoryImpl.Companion.EMAIL_COLUMN
import com.example.auth.domain.repository.CustomUserRepositoryImpl.Companion.NAME_COLUMN
import com.example.auth.domain.repository.CustomUserRepositoryImpl.Companion.PASSWORD_COLUMN
import com.example.auth.domain.repository.CustomUserRepositoryImpl.Companion.PROVIDER_COLUMN
import com.example.auth.domain.repository.CustomUserRepositoryImpl.Companion.ROLE_COLUMN
import com.example.auth.domain.repository.CustomUserRepositoryImpl.Companion.SOCIAL_ID_COLUMN
import com.fasterxml.uuid.Generators
import io.r2dbc.spi.Readable
import model.Role
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("user")
class User(
    @Column("id")
    @Id
    private val id: String = Generators.timeBasedEpochGenerator().generate().toString(),
    @Column("email")
    val email: String?,
    @Column("password")
    val password: String,
    @Column("social_id")
    val socialId: String?,
    @Column("provider")
    val provider: String?,
    @Column("role")
    val role: String,
    @Column("name")
    val name: String?,
) : Persistable<String> {
    @Column("created_at")
    @CreatedDate
    lateinit var createdAt: LocalDateTime

    @Column("updated_at")
    @LastModifiedDate
    lateinit var updatedAt: LocalDateTime

    @Column("deleted_at")
    var deletedAt: LocalDateTime? = null

    @Transient
    private var newEntity: Boolean = true

    override fun getId() = id

    @Transient
    override fun isNew(): Boolean = newEntity

    fun markNotNew() {
        newEntity = false
    }

    companion object {
        fun fromCommand(command: RegisterUserCommand): User =
            User(
                email = command.email,
                password = command.password,
                socialId = command.socialId,
                provider = command.provider?.name,
                role = Role.ROLE_USER.name,
                name = command.name,
            )

        fun fromRow(row: Readable): User =
            User(
                id = row.get("id", String::class.java)!!,
                socialId = row.get(SOCIAL_ID_COLUMN, String::class.java),
                provider = row.get(PROVIDER_COLUMN, String::class.java),
                password = row.get(PASSWORD_COLUMN, String::class.java) ?: "",
                email = row.get(EMAIL_COLUMN)?.toString(),
                role = row.get(ROLE_COLUMN).toString(),
                name = row.get(NAME_COLUMN).toString(),
            )
    }

    fun toModel(): model.User =
        model.User(
            id = id,
            email = email,
            password = password,
            provider = provider,
            role = Role.from(role),
            socialId = socialId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            name = name,
        )

    fun updateEmail(email: String?): String? {
        if (this.email != email) return email
        return null
    }

    fun updateName(name: String?): String? {
        if (this.name != name) return name
        return null
    }
}

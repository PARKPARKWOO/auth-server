package com.example.auth.domain.entity.application

import com.fasterxml.uuid.Generators
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table(name = "application")
class Application(
    @Id
    @Column("id")
    private val id: String = Generators.timeBasedEpochGenerator().generate().toString(),
    // unique
    @Column("name")
    val name: String,
    @Column("redirect_url")
    val redirectUrl: String,
    @Column("redirect_type")
    val redirectType: String,
    @Column("created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
) : Persistable<String> {
    companion object {
        fun create(
            name: String,
            redirectUrl: String,
            redirectType: String,
        ): Application {
            val application = Application(
                id = Generators.timeBasedEpochGenerator().generate().toString(),
                name = name,
                redirectUrl = redirectUrl,
                redirectType = redirectType,
                createdAt = LocalDateTime.now(),
            )
            application.newEntity = true
            return application
        }
    }

    @Transient
    private var newEntity: Boolean = false
    override fun getId() = id

    @Transient
    override fun isNew(): Boolean = newEntity
}

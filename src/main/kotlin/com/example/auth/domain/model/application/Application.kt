package com.example.auth.domain.model.application

import com.fasterxml.uuid.Generators
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(name = "application")
class Application(
    @Id
    @Column("id")
    private val id: String = Generators.timeBasedEpochGenerator().generate().toString(),
    // unique
    @Column("name")
    val name: String,
) : Persistable<String> {
    @Transient
    private var newEntity: Boolean = true
    override fun getId() = id

    @Transient
    override fun isNew(): Boolean = newEntity

    fun markNotNew() {
        newEntity = false
    }
}

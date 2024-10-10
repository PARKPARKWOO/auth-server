package com.example.auth.domain.model.application

import com.fasterxml.uuid.Generators
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(name = "application")
class Application(
    @Column("id")
    val id: String = Generators.timeBasedEpochGenerator().generate().toString(),
    // unique
    @Column("name")
    val name: String,
)

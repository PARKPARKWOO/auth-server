package com.example.auth.domain.entity.organization

import com.fasterxml.uuid.Generators
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("organization")
class Organization(
    @Id
    @Column("id")
    val id: String = Generators.timeBasedEpochGenerator().generate().toString(),
    @Column("name")
    val name: String,
    @Column("manager_id")
    val managerId: String,
) {
    @Column("created_at")
    @CreatedDate
    lateinit var createdAt: LocalDateTime
}
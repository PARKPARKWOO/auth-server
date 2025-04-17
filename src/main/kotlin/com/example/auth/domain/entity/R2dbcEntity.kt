package com.example.auth.domain.entity

import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable

abstract class R2dbcEntity<ID>(): Persistable<ID> {
    @Transient
    protected var newEntity: Boolean = false
    @Transient
    override fun isNew(): Boolean = newEntity
}
package com.example.auth.domain.repository

import com.example.auth.domain.entity.outbox.Outbox
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface OutboxRepository : ReactiveCrudRepository<Outbox, Long> {
}
package com.example.auth.domain.entity.outbox

import com.example.auth.domain.model.outbox.EventType
import com.example.auth.domain.model.outbox.RecordOperation
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table(name = "outbox")
class Outbox(
    @Id
    @Column("id")
    val id: Long,
    @Column("payload")
    val payload: String,
    @Column("event_type")
    val eventType: String,
//    @Column("status")
//    var status: String,
    @Column("record_operation")
    val recordOperation: String,
    @Column("created_at")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun create(
            payload: String,
            eventType: EventType,
            recordOperation: RecordOperation,
        ): Outbox =
            Outbox(
                id = 0L,
                payload = payload,
                eventType = eventType.name,
//                status = TransactionStatus.PENDING.name,

                recordOperation = recordOperation.name,
                createdAt = LocalDateTime.now(),
            )
    }
}

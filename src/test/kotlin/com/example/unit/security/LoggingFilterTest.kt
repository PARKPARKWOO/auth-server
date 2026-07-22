package com.example.unit.security

import com.example.auth.presentation.rest.filter.LoggingFilter
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import org.woo.apm.log.constant.ContextConstant
import reactor.core.publisher.Mono
import reactor.util.context.ContextView
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LoggingFilterTest {
    @Test
    fun `client request id and raw path never enter the reactor logging context`() {
        val rawUserId = "00000000-0000-4000-8000-000000000001"
        val rawPassport = "{\"userId\":\"$rawUserId\"}"
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest
                .get("/api/v1/admin/users/$rawUserId?invite=secret-token")
                .header("X-Request-ID", rawPassport, rawUserId)
                .build(),
        )
        val captured = AtomicReference<ContextView>()
        val downstreamRequestIds = AtomicReference<List<String>>()
        val chain = WebFilterChain { downstream ->
            downstreamRequestIds.set(downstream.request.headers["X-Request-ID"].orEmpty())
            Mono.deferContextual { context ->
                captured.set(context)
                Mono.empty()
            }
        }

        LoggingFilter().filter(exchange, chain).block()

        val context = captured.get()
        val traceId = context.get<String>(ContextConstant.TRACE_ID)
        assertEquals(traceId, UUID.fromString(traceId).toString())
        assertFalse(traceId.contains(rawUserId))
        assertFalse(traceId.contains("userId"))
        assertEquals(listOf(traceId), downstreamRequestIds.get())
        assertEquals("[REDACTED]", context.get<String>(ContextConstant.PATH))
        assertFalse(context.toString().contains(rawPassport))
        assertFalse(context.toString().contains(rawUserId))
        assertFalse(context.toString().contains("secret-token"))
    }
}

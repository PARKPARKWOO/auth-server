package com.example.auth.common.context

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.withContext
import org.woo.log.constant.ContextConstant.IS_MOBILE
import org.woo.log.constant.ContextConstant.TRACE_ID
import reactor.core.publisher.Mono
import reactor.util.context.Context

object RequestContextUtil {
    suspend fun getContext(): Context? =
        kotlin.coroutines.coroutineContext[ReactorContext]
            ?.context

    suspend inline fun isMobileDevice(): Boolean = getContext()?.getOrDefault(IS_MOBILE, false) as Boolean

    suspend inline fun getTraceId(): String {
        return getContext()?.getOrDefault(TRACE_ID, "unknown") ?: ""
    }
}

suspend fun <T> withReactorContext(block: suspend () -> T): T {
    val reactorContext = Mono.deferContextual { Mono.just(it) }.awaitFirstOrNull()
    return if (reactorContext != null) {
        withContext(ReactorContext(reactorContext)) {
            block()
        }
    } else {
        block()
    }
}

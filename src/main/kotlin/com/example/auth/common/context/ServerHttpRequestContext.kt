package com.example.auth.common.context

import com.example.auth.common.constants.ContextConstant.IS_MOBILE
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.http.server.reactive.ServerHttpRequest
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class ServerHttpRequestContext(val request: ServerHttpRequest) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ServerHttpRequestContext>
}

suspend fun getServerHttpRequest(): ServerHttpRequest {
    return coroutineContext[ServerHttpRequestContext]?.request ?: error("No ServerHttpRequest found in context")
}

suspend fun isMobileDevice(): Boolean {
    val reactorContext = coroutineContext[ReactorContext]
        ?: error("ReactorContext not found in CoroutineContext")
    return reactorContext.context.getOrDefault(IS_MOBILE, false) ?: false
}

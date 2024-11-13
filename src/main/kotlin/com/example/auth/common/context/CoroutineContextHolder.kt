package com.example.auth.common.context

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.springframework.http.server.reactive.ServerHttpRequest

open class CoroutineContextHolder {
    suspend fun <T> withRequestContext(request: ServerHttpRequest, block: suspend CoroutineScope.() -> T): T {
        return withContext(ServerHttpRequestContext(request), block)
    }
}

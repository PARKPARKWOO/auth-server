package com.example.auth.presentation.filter

import com.example.auth.common.constants.ContextConstant
import com.example.auth.common.constants.ContextConstant.IS_MOBILE
import com.example.auth.common.constants.ContextConstant.TRACE_ID
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

@Component
@Order(-1)
class LoggingFilter : WebFilter {
    private fun isMobileDevice(userAgent: String): Boolean {
        val mobileKeywords =
            listOf("Android", "iPhone", "iPad", "iPod", "BlackBerry", "Windows Phone", "Opera Mini", "IEMobile")
        return mobileKeywords.any { userAgent.contains(it, ignoreCase = true) }
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val userAgent = request.headers.getFirst("User-Agent") ?: ""
        val isMobile = isMobileDevice(userAgent)
        val traceId = request.headers.getFirst("X-Request-ID") ?: UUID.randomUUID().toString()
//        val map = ConcurrentHashMap(
//            mapOf(
//                TRACE_ID to traceId,
//                IS_MOBILE to isMobile,
//            ),
//        )
        return chain.filter(exchange).contextWrite { context ->
            context.put(IS_MOBILE, isMobile)
                .put(TRACE_ID, traceId)
                .put(ContextConstant.PATH, request.path.toString())
                .put(ContextConstant.METHOD, request.method.name())
        }
    }
}

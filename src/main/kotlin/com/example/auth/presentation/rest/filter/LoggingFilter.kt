package com.example.auth.presentation.rest.filter

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import org.woo.apm.log.constant.ContextConstant
import org.woo.apm.log.filter.AbstractReactiveMDCInitializationFilter
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LoggingFilter : AbstractReactiveMDCInitializationFilter() {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val traceId = UUID.randomUUID().toString()
        val sanitizedRequest =
            exchange.request
                .mutate()
                .headers { headers ->
                    headers.remove(ContextConstant.TRACE_ID)
                    headers.set(ContextConstant.TRACE_ID, traceId)
                }.build()
        val sanitizedExchange = exchange.mutate().request(sanitizedRequest).build()

        return chain.filter(sanitizedExchange).contextWrite { context ->
            customizeContext(context, sanitizedExchange)
                .put(ContextConstant.TRACE_ID, traceId)
                .put(ContextConstant.PATH, "[REDACTED]")
                .put(ContextConstant.METHOD, sanitizedRequest.method.name())
        }
    }

    private fun isMobileDevice(userAgent: String): Boolean {
        val mobileKeywords =
            listOf("Android", "iPhone", "iPad", "iPod", "BlackBerry", "Windows Phone", "Opera Mini", "IEMobile")
        return mobileKeywords.any { userAgent.contains(it, ignoreCase = true) }
    }

    override fun customizeContext(
        context: Context,
        exchange: ServerWebExchange,
    ): Context {
        val request = exchange.request
        val userAgent = request.headers.getFirst("User-Agent") ?: ""
        val isMobile = isMobileDevice(userAgent)
        return super
            .customizeContext(context, exchange)
            .put(ContextConstant.IS_MOBILE, isMobile)
    }
}

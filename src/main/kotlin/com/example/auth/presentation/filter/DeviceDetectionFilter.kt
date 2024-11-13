package com.example.auth.presentation.filter

import com.example.auth.common.constants.ContextConstant.IS_MOBILE
import com.example.auth.common.context.ServerHttpRequestContext
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class DeviceDetectionFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val userAgent = exchange.request.headers.getFirst("User-Agent") ?: ""
        val isMobile = isMobileDevice(userAgent)
        println(userAgent)
        return chain.filter(exchange)
            .contextWrite { context ->
                context.put(ServerHttpRequestContext.Key, exchange.request)
                    .put(IS_MOBILE, isMobile)
            }
    }

    private fun isMobileDevice(userAgent: String): Boolean {
        val mobileKeywords =
            listOf("Android", "iPhone", "iPad", "iPod", "BlackBerry", "Windows Phone", "Opera Mini", "IEMobile")
        return mobileKeywords.any { userAgent.contains(it, ignoreCase = true) }
    }
}

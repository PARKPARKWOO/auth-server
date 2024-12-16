package com.example.auth.presentation.rest.filter

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.woo.log.constant.ContextConstant
import org.woo.log.filter.AbstractReactiveMDCInitializationFilter
import reactor.util.context.Context

@Component
@Order(-1)
class LoggingFilter : AbstractReactiveMDCInitializationFilter() {
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
        println(request.path)
        val userAgent = request.headers.getFirst("User-Agent") ?: ""
        val isMobile = isMobileDevice(userAgent)
        return super
            .customizeContext(context, exchange)
            .put(ContextConstant.IS_MOBILE, isMobile)
    }
}

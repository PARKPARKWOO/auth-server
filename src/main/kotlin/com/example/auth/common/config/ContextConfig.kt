package com.example.auth.common.config

import com.example.auth.common.context.CoroutineContextHolder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.context.annotation.RequestScope

@Configuration
class ContextConfig {
    @Bean
    @RequestScope
    fun coroutineContextHolder(): CoroutineContextHolder = CoroutineContextHolder()
}

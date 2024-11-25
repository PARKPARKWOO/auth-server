package com.example.auth.common.config

import com.example.auth.presentation.filter.LoggingFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.WebFilter

@Configuration
class FilterConfig {
    @Bean
    fun loggingFilter(): LoggingFilter = LoggingFilter()
}

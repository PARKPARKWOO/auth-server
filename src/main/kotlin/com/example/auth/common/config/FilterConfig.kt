package com.example.auth.common.config

import com.example.auth.presentation.rest.resolver.PassportArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

@Configuration
@EnableWebFlux
class FilterConfig(
    private val passportArgumentResolver: PassportArgumentResolver,
) : WebFluxConfigurer {
    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        configurer.addCustomResolver(passportArgumentResolver)
    }
}

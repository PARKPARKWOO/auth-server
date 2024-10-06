package com.example.auth.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.csrf { csrf -> csrf.disable() }
            .authorizeExchange { exchange ->
                exchange.pathMatchers("/api/v1/auth/**").permitAll()
                    .pathMatchers(HttpMethod.OPTIONS).permitAll()
                    .anyExchange().authenticated()
            }
            .httpBasic(Customizer.withDefaults())
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
//                oauth2.jwt { jwt ->
//                    jwt.jwtAuthenticationConverter(jwtConverter())
//                }
            }
            .build()
    }

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type")
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }

        return CorsWebFilter(source)
    }

//    @Bean
//    fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): ReactiveJwtDecoder {
//        return NimbusReactiveJwtDecoder.withJwkSource(jwkSource).build()
//    }

//    private fun jwtConverter(): ReactiveJwtAuthenticationConverterAdapter {
//        val adapter = ReactiveJwtAuthenticationConverterAdapter { jwt ->
//            val authorities: List<String> = jwt.claims["authorities"]?.let {
//                it as List<String>
//            } ?: emptyList()
//
//            UsernamePasswordAuthenticationToken(
//                jwt.subject,
//                "n/a",
//                authorities.map { SimpleGrantedAuthority(it) },
//            )
//        }
//        return adapter
//    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}

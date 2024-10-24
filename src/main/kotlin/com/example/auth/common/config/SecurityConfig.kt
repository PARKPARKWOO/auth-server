package com.example.auth.common.config

import com.example.auth.business.service.JwtTokenGenerator
import com.example.auth.business.service.oauth.OAuthAuthenticationSuccessHandler
import com.example.auth.common.constants.AuthConstants
import com.example.auth.domain.repository.DynamicReactiveClientRegistrationRepository
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.DelegatingReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeReactiveAuthenticationManager
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
    @Qualifier("oauth2LoginAuthenticationManager")
    private val oauth2LoginAuthenticationManager: DelegatingReactiveAuthenticationManager,
    private val oauth2ClientAuthenticationManager: OAuth2AuthorizationCodeReactiveAuthenticationManager,
    private val dynamicReactiveClientRegistrationRepository: DynamicReactiveClientRegistrationRepository,
    private val jwtTokenGenerator: JwtTokenGenerator,
    @Value("\${jwt.access-token.secret-key}")
    private val accessTokenSecretKeyString: String,
) {
    private val accessTokenSecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessTokenSecretKeyString))

    companion object {
        val SWAGGER_WHITELIST = arrayOf(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
        )
    }

    @Bean
    fun filterChain(
        http: ServerHttpSecurity,
    ): SecurityWebFilterChain {
        return http.csrf { csrf -> csrf.disable() }
            .authorizeExchange { exchange ->
                exchange.pathMatchers("/**", "/login").permitAll() // 로그인 페이지와 루트는 허용
                exchange.pathMatchers("/api/v1/auth/**").permitAll()
                    .pathMatchers(*SWAGGER_WHITELIST).permitAll()
                    .pathMatchers(HttpMethod.OPTIONS).permitAll()
                    .anyExchange().authenticated()
            }
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .oauth2ResourceServer { oauth2 ->
//                oauth2.jwt(Customizer.withDefaults())
                oauth2.jwt { jwt ->
                    jwt.jwtDecoder(reactiveJwtDecoder())
                    jwt.jwtAuthenticationConverter(jwtConverter())
                }
            }
            .oauth2Login { oauth2Login ->
                oauth2Login.authenticationManager(oauth2LoginAuthenticationManager)
                oauth2Login.clientRegistrationRepository(dynamicReactiveClientRegistrationRepository)
                oauth2Login.authenticationSuccessHandler(oAuth2AuthenticationSuccessHandler())
            }
            .oauth2Client { oauth2Client ->
                oauth2Client.authenticationManager(oauth2ClientAuthenticationManager)
            }
            .build()
    }

    @Bean
    fun oAuth2AuthenticationSuccessHandler(): ServerAuthenticationSuccessHandler {
        return OAuthAuthenticationSuccessHandler(jwtTokenGenerator)
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
//    fun jwtDecoder(): ReactiveJwtDecoder {
//        return NimbusReactiveJwtDecoder.withPublicKey(rsaPublicKey())
//            .build()
//    }
    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder {
        return NimbusReactiveJwtDecoder.withSecretKey(accessTokenSecretKey).macAlgorithm(MacAlgorithm.HS512).build()
    }

    @Bean
    fun jwkSource(): JWKSource<SecurityContext> {
        val keyPair: KeyPair = generateRsaKey()
        val publicKey: RSAPublicKey = keyPair.public as RSAPublicKey
        val privateKey: RSAPrivateKey = keyPair.private as RSAPrivateKey
        val rsaKey: RSAKey = RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build()
        val jwkSet = JWKSet(rsaKey)
        return ImmutableJWKSet(jwkSet)
    }

    private fun generateRsaKey(): KeyPair =
        try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            keyPairGenerator.generateKeyPair()
        } catch (ex: Exception) {
            throw IllegalStateException(ex)
        }

    private fun jwtConverter(): ReactiveJwtAuthenticationConverterAdapter {
        val adapter = ReactiveJwtAuthenticationConverterAdapter { jwt ->
            val authorities: List<String> = jwt.claims[AuthConstants.USER_ROLE]?.let {
                it as List<String>
            } ?: emptyList()

            UsernamePasswordAuthenticationToken(
                jwt.subject,
                "n/a",
                authorities.map { SimpleGrantedAuthority(it) },
            )
        }
        return adapter
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}

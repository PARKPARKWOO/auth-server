package com.example.auth.presentation.rest.resolver

import annotation.AuthenticationUser
import com.example.auth.business.exception.BusinessException
import com.example.auth.common.http.error.ErrorCode
import dto.Passport
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import org.woo.apm.log.log
import org.woo.mapper.Jackson
import reactor.core.publisher.Mono

/**
 * LOGOUT-1: WebFlux 용 Passport ArgumentResolver.
 *
 * 게이트웨이의 AuthenticateGrpcFilter 가 토큰을 검증한 뒤 `X-User-Passport` 헤더에 박아준다.
 * 이 리졸버는 그 헤더를 역직렬화해 `@AuthenticationUser passport: Passport` 파라미터에 주입.
 *
 * 보안 전제: 게이트웨이가 클라이언트 위조 헤더를 strip 한다 (X-User-Passport 헤더는
 * 게이트웨이 발행분만 도달 — `prd/platform/api-spec.md` 6.2 참조).
 */
@Component
class PassportArgumentResolver : HandlerMethodArgumentResolver {
    companion object {
        private const val PASSPORT_HEADER = "X-User-Passport"
    }

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(AuthenticationUser::class.java) &&
            parameter.parameterType == Passport::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        bindingContext: BindingContext,
        exchange: ServerWebExchange,
    ): Mono<Any> {
        val isRequired =
            parameter.getParameterAnnotation(AuthenticationUser::class.java)?.isRequired ?: true
        val passport = readPassport(exchange)

        return when {
            passport != null -> Mono.just(passport)
            isRequired -> Mono.error(BusinessException(ErrorCode.FORBIDDEN, null))
            else -> Mono.empty()
        }
    }

    private fun readPassport(exchange: ServerWebExchange): Passport? {
        val raw = exchange.request.headers.getFirst(PASSPORT_HEADER)
        if (raw.isNullOrBlank()) return null
        return try {
            Jackson.readValue(raw, Passport::class.java)
        } catch (e: Exception) {
            log().warn("failed to parse $PASSPORT_HEADER: ${e.message}")
            null
        }
    }
}

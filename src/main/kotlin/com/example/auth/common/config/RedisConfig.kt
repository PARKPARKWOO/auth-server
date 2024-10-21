package com.example.auth.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.redisson.Redisson
import org.redisson.api.RedissonReactiveClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig(
    @Value("\${spring.data.redis.host}")
    val host: String,
    @Value("\${spring.data.redis.port}")
    val port: Int,
    @Value("\${spring.data.redis.password}")
    val password: String,
) {
    companion object {
        const val REDISSON_PREFIX = "redis://"
    }

    @Bean("reactiveRedisConnectionFactory")
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        val config = RedisStandaloneConfiguration(host, port)
        config.setPassword(RedisPassword.of(password))
        return LettuceConnectionFactory(config)
    }

    @Bean
    fun redisTemplate(@Qualifier("reactiveRedisConnectionFactory") redisConnectionFactory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Any> {
//        val rrcf: ReactiveRedisConnectionFactory = redisConnectionFactory
//        val serializer = Jackson2JsonRedisSerializer(Any::class.java)
//        val builder = RedisSerializationContext
//            .newSerializationContext<String, Any>(StringRedisSerializer())
//        val context = builder.value(serializer).hashValue(serializer)
//            .hashKey(serializer).build()
        val rrcf: ReactiveRedisConnectionFactory = redisConnectionFactory

        // String 직렬화기 (key용)
        val stringSerializer = StringRedisSerializer()
        val mapper = ObjectMapper().apply {
            findAndRegisterModules()
        }
        // JSON 직렬화기 (value용)
        val jacksonSerializer = Jackson2JsonRedisSerializer(mapper, Any::class.java)

        val builder = RedisSerializationContext
            .newSerializationContext<String, Any>(stringSerializer)
        val context = builder
            .value(jacksonSerializer)
            .hashKey(stringSerializer)
            .hashValue(jacksonSerializer)
            .build()

        return ReactiveRedisTemplate(rrcf, context)
    }

    @Bean
    fun redissonClient(): RedissonReactiveClient {
        val config = org.redisson.config.Config()
        config.useSingleServer().setAddress("$REDISSON_PREFIX$host:$port")
        config.useSingleServer().setPassword(password)
        return Redisson.create(config).reactive()
    }
}

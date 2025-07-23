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
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
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
        config.password = RedisPassword.of(password)
        return LettuceConnectionFactory(config)
    }

    @Bean("objectReactiveRedisTemplate")
    @Primary
    fun objectReactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Any> {
        val stringSerializer = StringRedisSerializer()
        val mapper = ObjectMapper().apply {
            findAndRegisterModules()
            activateDefaultTyping(polymorphicTypeValidator, ObjectMapper.DefaultTyping.NON_FINAL)
        }
        val jsonSerializer = GenericJackson2JsonRedisSerializer(mapper)
        val context = RedisSerializationContext.newSerializationContext<String, Any>(stringSerializer)
            .value(jsonSerializer)
            .hashKey(stringSerializer)
            .hashValue(jsonSerializer)
            .build()

        return ReactiveRedisTemplate(factory, context)
    }

    @Bean("stringReactiveRedisTemplate")
    fun stringReactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, String> {
        val stringSerializer = StringRedisSerializer()
        val context = RedisSerializationContext.newSerializationContext<String, String>(stringSerializer)
            .key(stringSerializer)
            .value(stringSerializer)
            .hashKey(stringSerializer)
            .hashValue(stringSerializer)
            .build()

        return ReactiveRedisTemplate(factory, context)
    }

    @Bean
    fun redissonClient(): RedissonReactiveClient {
        val config = org.redisson.config.Config()
        config.useSingleServer().setAddress("$REDISSON_PREFIX$host:$port")
        config.useSingleServer().setPassword(password)
        return Redisson.create(config).reactive()
    }
}

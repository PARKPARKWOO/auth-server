package com.example.auth.domain.repository.redis

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisDriver(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
) {
    suspend fun <T> setValue(key: String, value: T, ttl: Long) {
        if (value != null) {
            redisTemplate.opsForValue().set(key, value, ttl).awaitSingle()
        }
        // null 처리 필요한가
    }

    suspend fun <T> getValue(key: String, clazz: Class<T>): T? {
        val value = redisTemplate.opsForValue().get(key).awaitSingle()
        return clazz.cast(value)
    }

    suspend fun <T> addListForRight(key: String, value: List<T>) {
        redisTemplate.opsForList().rightPush(key, value)
            .awaitSingle()
    }

    suspend fun <T> getList(key: String, clazz: Class<T>): Flow<T> {
        return redisTemplate.opsForList()
            .range(key, 0, -1)
            .asFlow()
            .map { clazz.cast(it) }
    }

    suspend fun <T> addSetForSingle(key: String, value: T) {
        redisTemplate.opsForSet()
            .add(key, value)
            .awaitSingle()
    }

    suspend fun <T> addSetForMultiValue(key: String, value: Array<T>) {
        redisTemplate.opsForSet()
            .add(key, *value)
            .awaitSingle()
    }

    suspend fun <T> updateSetForSingle(key: String, previousValue: T, currentValue: T) {
        redisTemplate.opsForSet()
            .isMember(key, previousValue)
            .asFlow().collect {
                it.remove(it.keys)
            }
        redisTemplate.opsForSet()
            .add(key, currentValue)
            .awaitSingle()
    }

    suspend fun <T> setFindAll(key: String, clazz: Class<T>): Set<T> {
        return redisTemplate.opsForSet()
            .members(key)
            .asFlow()
            .map { ObjectMapper().convertValue(it, object : TypeReference<T>() {}) }
            .toSet()
    }
    // 같은 값이더라도 생성되면 Id 값이 달라 Set 에 계속 저장된다.
}

package com.example.auth.domain.repository.redis

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.redisson.api.RedissonReactiveClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisDriver(
    @Qualifier("objectReactiveRedisTemplate")
    private val objectRedisTemplate: ReactiveRedisTemplate<String, Any>,
    @Qualifier("stringReactiveRedisTemplate")
    private val stringRedisTemplate: ReactiveRedisTemplate<String, String>,
    private val redissonClient: RedissonReactiveClient,
) {
    suspend fun <T> setValue(key: String, value: T, ttl: Long) {
        if (value != null) {
            objectRedisTemplate.opsForValue().set(key, value, ttl).awaitSingle()
        }
    }

    suspend fun setValue(key: String, value: String, ttl: Long) {
        stringRedisTemplate.opsForValue().set(key, value, ttl).awaitSingle()
    }

    suspend fun <T> getValue(key: String, clazz: Class<T>): T? {
        val value = objectRedisTemplate.opsForValue().get(key).awaitSingleOrNull()
        return value?.let { clazz.cast(it) }
    }

    suspend fun getValue(key: String, ttl: Long): String? =
        stringRedisTemplate.opsForValue().get(key).awaitSingleOrNull()


    suspend fun <T> addListForRight(key: String, value: List<T>) {
        objectRedisTemplate.opsForList().rightPush(key, value)
            .awaitSingle()
    }

    suspend fun <T> addListForRight(key: String, value: T) {
        objectRedisTemplate.opsForList().rightPush(key, value!!)
            .awaitSingle()
    }

    suspend fun <T> getList(key: String, clazz: Class<T>): Flow<T> {
        return objectRedisTemplate.opsForList()
            .range(key, 0, -1)
            .asFlow()
            .map { clazz.cast(it) }
    }

    suspend fun <T> addSetForSingle(key: String, value: T) {
        objectRedisTemplate.opsForSet()
            .add(key, value)
            .awaitSingle()
    }

    suspend fun <T> addSetForMultiValue(key: String, value: Array<T>) {
        objectRedisTemplate.opsForSet()
            .add(key, *value)
            .awaitSingle()
    }

    suspend fun <T> updateSetForSingle(key: String, previousValue: T, currentValue: T) {
        objectRedisTemplate.opsForSet()
            .isMember(key, previousValue)
            .map {
                it.values.forEach { isMember ->
                    if (isMember) {
                        objectRedisTemplate.opsForSet()
                            .remove(key, previousValue)
                            .asFlow()
                    }
                }
            }.awaitSingle()
        objectRedisTemplate.opsForSet()
            .add(key, currentValue)
            .awaitSingle()
    }

    suspend fun <T> setFindAll(key: String, clazz: Class<T>): Set<T> {
        return objectRedisTemplate.opsForSet()
            .members(key)
            .asFlow()
            .map { ObjectMapper().convertValue(it, object : TypeReference<T>() {}) }
            .toSet()
    }

    suspend fun tryLock(key: String, waitTimeMs: Long, leaseTimeMs: Long): Boolean {
        val lock = redissonClient.getLock(key)
        return lock.tryLock(waitTimeMs, leaseTimeMs, TimeUnit.MILLISECONDS).awaitSingleOrNull()
            ?: false
    }

    suspend fun unlock(key: String) {
        val lock = redissonClient.getLock(key)
        try {
            lock.unlock().awaitSingleOrNull()
        } catch (ignore: IllegalMonitorStateException) {
        }
    }

    suspend fun <T> useLockOrNull(
        key: String,
        waitTimeMs: Long,
        leaseTimeMs: Long,
        action: suspend () -> T
    ): T? {

        val acquired = tryLock(key, waitTimeMs, leaseTimeMs)

        if (!acquired) {
            return null
        }
        try {
            return action()
        } finally {
            unlock(key)
        }
    }
}

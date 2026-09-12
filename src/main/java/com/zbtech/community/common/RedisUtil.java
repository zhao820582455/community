package com.zbtech.community.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 对 StringRedisTemplate 的轻量封装（对齐原 foxbook Redis 用法）
 */
@Component
public class RedisUtil {

    private final StringRedisTemplate redis;

    public RedisUtil(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void set(String key, String value, long ttlSeconds) {
        if (ttlSeconds > 0) {
            redis.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } else {
            redis.opsForValue().set(key, value);
        }
    }

    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    public void delete(String key) {
        if (key != null) {
            redis.delete(key);
        }
    }

    public boolean hasKey(String key) {
        Boolean b = redis.hasKey(key);
        return Boolean.TRUE.equals(b);
    }

    public long getExpire(String key) {
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        return ttl == null ? -1 : ttl;
    }
}

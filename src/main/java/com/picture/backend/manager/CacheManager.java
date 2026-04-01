package com.picture.backend.manager;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class CacheManager {
    @Autowired
    private StringRedisTemplate redisTemplate;
    // 本地缓存池
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    // 简单缓存，检查多级缓存是否存在
    public <T> T inspectMutiLevelCache(String key, Class<T> clazz) {
        // 1、先检查本地缓存
        String cacheValue = LOCAL_CACHE.getIfPresent(key);
        // 2、缓存存在直接返回
        if (cacheValue != null){
            return JSONUtil.toBean(cacheValue, clazz);
        }
        // 3、本地缓存不存在，检查redis缓存
        String redisValue = redisTemplate.opsForValue().get(key);
        if (redisValue != null){
            LOCAL_CACHE.put(key, redisValue);
            return JSONUtil.toBean(redisValue, clazz);
        }
        // 4、redis缓存不存在则返回null
        return null;
    }

    // 复杂缓存，检查多级缓存是否存在
    public <T> T inspectMutiLevelCache(String key, TypeReference<T> typeReference) {
        // 1、先检查本地缓存
        String cacheValue = LOCAL_CACHE.getIfPresent(key);
        // 2、缓存存在直接返回
        if (cacheValue != null){
            return JSONUtil.parseObj(cacheValue).toBean(typeReference);
        }
        // 3、本地缓存不存在，检查redis缓存
        String redisValue = redisTemplate.opsForValue().get(key);
        if (redisValue != null){
            LOCAL_CACHE.put(key, redisValue);
            return JSONUtil.parseObj(redisValue).toBean(typeReference);
        }
        // 4、redis缓存不存在则返回null
        return null;
    }

    // 创建多级缓存
    public void createMultiLevelCache(String key, Object object) {
        // 1、将数据库结果放入本地缓存
        String cacheValue = JSONUtil.toJsonStr(object);
        LOCAL_CACHE.put(key, cacheValue);
        // 2、将数据库结果放入redis缓存
        int expireTime = RandomUtil.randomInt(6, 10);
        redisTemplate.opsForValue().set(key, cacheValue, expireTime, TimeUnit.MINUTES);
    }

    // 创建带过期时间的缓存（用于空结果）
    public void createNullCache(String key,Object object ,int expireMinutes) {
        String cacheValue = JSONUtil.toJsonStr(object);
        redisTemplate.opsForValue().set(key, cacheValue, expireMinutes, TimeUnit.SECONDS);
    }

    // 删除缓存
    public void deleteCache(String key) {
        LOCAL_CACHE.invalidate(key);
        redisTemplate.delete(key);
    }

    // 根据前缀批量删除缓存
    public void batchDeleteCache(String prefix) {
        // 删除redis缓存
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        // 删除本地缓存
        LOCAL_CACHE.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }

    // 删除所有缓存
    public String deleteAllCache() {
        LOCAL_CACHE.invalidateAll();
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.flushDb();
            return null;
        });
        return "All cache deleted";
    }


}


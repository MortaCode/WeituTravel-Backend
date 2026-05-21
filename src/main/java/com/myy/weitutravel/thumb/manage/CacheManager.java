package com.myy.weitutravel.thumb.manage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.myy.weitutravel.thumb.service.HeavyKeeper;
import com.myy.weitutravel.thumb.service.TopK;
import com.myy.weitutravel.thumb.vo.AddResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheManager {

    private TopK hotKeyDetector;

    private Cache<String, Object> localCache;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Bean
    public TopK getHotKeyDetector() {
        hotKeyDetector = new HeavyKeeper(
                // 监控 Top 100 Key
                100,
                // 哈希表宽度
                100000,
                // 哈希表深度
                5,
                // 衰减系数
                0.92,
                // 最小出现 10 次才记录
                10
        );
        return hotKeyDetector;
    }

    @Bean
    public Cache<String, Object> localCache() {
        return localCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    // 辅助方法：构造复合 key
    private String buildCacheKey(String redisKey, String fieldKey) {
        return redisKey + ":" + fieldKey;
    }

    public Object get(String redisKey, String fieldKey) {
        // 构造唯一的 composite key/ˈkɒmpəzɪt/
        String compositeKey = buildCacheKey(redisKey, fieldKey);

        // 1. 先查本地缓存
        Object value = localCache.getIfPresent(compositeKey);
        if (value != null) {
            log.info("本地缓存获取到数据 {} = {}", compositeKey, value);
            hotKeyDetector.add(fieldKey, 1);
            return value;
        }

        // 2. 本地缓存未命中，查询 Redis
        Object redisValue = redisTemplate.opsForHash().get(redisKey, fieldKey);
        if (redisValue != null) {
            log.info("Redis缓存获取到数据 {} = {}", compositeKey, value);
            AddResult addResult = hotKeyDetector.add(fieldKey, 1);
            if (addResult.isHotKey()) {
                localCache.put(compositeKey, redisValue);
            }
        }

        // 3. 数据库查询
        return redisValue;
    }

    /**
     * 存在才更新。本地缓存只保留热点数据
     * @param hashKey
     * @param key
     * @param value
     */
    public void putIfPresent(String hashKey, String key, Object value) {
        String compositeKey = buildCacheKey(hashKey, key);
        Object object = localCache.getIfPresent(compositeKey);
        if (object == null) {
            return;
        }
        localCache.put(compositeKey, value);
    }

    // 定时清理过期的热 Key 检测数据
    @Scheduled(fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    public void cleanHotKeys() {
        hotKeyDetector.fading();
    }
}

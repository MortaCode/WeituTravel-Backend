package com.myy.weitutravel.flashSale.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;

/**
 * 第一层防线：Redis预扣减
 * 使用Lua脚本保证原子性，无锁化设计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisPreDeductService {

    private final StringRedisTemplate redisTemplate;

    private RedisScript<Long> stockDeductScript;

    @PostConstruct
    public void init() {
        // Lua脚本：原子性扣减库存并返回剩余库存
        String script =
                "local key = KEYS[1] " +
                        "local stock = redis.call('get', key) " +
                        "if stock and tonumber(stock) > 0 then " +
                        "   local newStock = redis.call('decr', key) " +
                        "   return newStock " +
                        "else " +
                        "   return -1 " +
                        "end";

        stockDeductScript = new DefaultRedisScript<>(script, Long.class);
    }

    /**
     * 预热库存到Redis
     */
    public void warmUpStock(Long stockId, int stock) {
        String key = buildStockKey(stockId);
        redisTemplate.opsForValue().set(key, String.valueOf(stock));
        log.info("预热库存完成: stockId={}, stock={}", stockId, stock);
    }

    /**
     * 原子扣减库存
     * @return true-扣减成功，false-库存不足
     */
    public boolean tryDeductStock(Long stockId) {
        String key = buildStockKey(stockId);
        Long result = redisTemplate.execute(
                stockDeductScript,
                Collections.singletonList(key)
        );

        boolean success = result != null && result >= 0;
        if (success) {
            log.info("Redis预扣成功: stockId={}, 剩余库存={}", stockId, result);
        } else {
            log.warn("Redis预扣失败，库存不足: stockId={}", stockId);
        }
        return success;
    }

    /**
     * 获取当前剩余库存
     */
    public Long getCurrentStock(Long roomId) {
        String stockStr = redisTemplate.opsForValue().get(buildStockKey(roomId));
        if (!StringUtils.hasText(stockStr)) {
            return 0L;
        }
        return Long.parseLong(stockStr);
    }

    private String buildStockKey(Long stockId) {
        return "stock:" + stockId;
    }
}
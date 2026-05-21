package com.myy.weitutravel.flashSale.controller;

import com.myy.weitutravel.common.api.Result;
import com.myy.weitutravel.flashSale.service.DatabaseUpdateService;
import com.myy.weitutravel.flashSale.service.RedisPreDeductService;
import com.myy.weitutravel.flashSale.vo.BookingRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 秒杀
 */
@Slf4j
@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final RedisPreDeductService redisPreDeductService;
    private final DatabaseUpdateService databaseUpdateService;

    @Value("${booking.stock-id}")
    private Long stockId;

    @Value("${booking.total-stock}")
    private Integer totalStock;

    // 统计计数器
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount = new AtomicLong(0);

    /**
     * 库存预热初始化数据
     */
    @PostConstruct
    public void init() {
        // 启动时预热库存
        redisPreDeductService.warmUpStock(stockId, totalStock);
        log.info("系统初始化完成，预热库存: {} 个", totalStock);
    }

    /**
     * 预订接口 - 模拟1万并发
     * 流程：
     * 1. Redis预扣减（第一道防线，抗98%以上并发）
     * 2. 发送MQ消息异步处理（第二道防线，削峰填谷）
     * 3. 立即返回处理中状态
     */
    @PostMapping("/book")
    public Result<String> book(@RequestBody BookingRequest request) {

        // 第一层防线：Redis预扣减（原子操作）
        boolean redisSuccess = redisPreDeductService.tryDeductStock(stockId);

        if (!redisSuccess) {
            failCount.incrementAndGet();
            log.warn("Redis预扣失败，库存不足: userId={}", request.getUserId());
            return Result.error("库存不足，抢购失败");
        }

        // 第二层防线：发送MQ消息异步处理
        // 注意：这里不入库，直接返回成功，真正落库由MQ消费者完成
        //mqConsumerService.sendOrderMessage(request, request.getUserId());
        databaseUpdateService.deductStockWithOptimisticLock(request.getStockId());

        successCount.incrementAndGet();
        log.info("抢购成功，进入排队: userId={}", request.getUserId());

        return Result.success("抢购成功，正在处理中");
    }

    /**
     * 查询当前状态
     */
    @GetMapping("/stats")
    public String getStats() {
        Long currentStock = redisPreDeductService.getCurrentStock(stockId);
        return String.format("成功: %d, 失败: %d, Redis剩余库存: %d, 已售: %d",
                successCount.get(), failCount.get(),
                currentStock, totalStock - currentStock);
    }
}

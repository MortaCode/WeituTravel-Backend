package com.myy.weitutravel.flashSale.service;//package com.myy.shop.book.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
//import org.apache.rocketmq.spring.core.RocketMQListener;
//import org.springframework.stereotype.Component;
//import tools.jackson.databind.ObjectMapper;
//
//import java.util.Map;
//import java.util.concurrent.Semaphore;
//
///**
// * 消费者：批量处理订单，缓慢落库
// */
//@Slf4j
//@Component
//@RocketMQMessageListener(
//        topic = "hotel_order_topic",
//        selectorExpression = "create_order",
//        consumerGroup = "hotel-order-consumer-group",
//        consumeThreadMax = 10
//)
//@RequiredArgsConstructor
//public class OrderConsumerService implements RocketMQListener<String> {
//
//    private final DatabaseUpdateService databaseUpdateService;
//    private final ObjectMapper objectMapper;
//
//    // 限流：同时最多处理10个订单
//    private final Semaphore semaphore = new Semaphore(10);
//
//    @Override
//    public void onMessage(String message) {
//        if (!semaphore.tryAcquire()) {
//            log.warn("系统繁忙，订单稍后处理: {}", message);
//            return;
//        }
//
//        try {
//            Map<String, Object> orderData = objectMapper.readValue(message, Map.class);
//            String roomId = orderData.get("roomId").toString();
//            String userId = orderData.get("userId").toString();
//
//            // 缓慢落库（模拟数据库处理延迟）
//            Thread.sleep(50);
//
//            boolean success = databaseUpdateService.deductStockWithOptimisticLock(roomId);
//
//            if (success) {
//                log.info("订单创建成功: roomId={}, userId={}", roomId, userId);
//                // 这里可以发送成功通知给用户
//            } else {
//                log.warn("订单创建失败，库存不足: roomId={}, userId={}", roomId, userId);
//                // 这里可以发送失败通知，并回滚Redis库存
//                rollbackRedisStock(roomId);
//            }
//
//        } catch (Exception e) {
//            log.error("处理订单消息失败: {}", message, e);
//        } finally {
//            semaphore.release();
//        }
//    }
//
//    private void rollbackRedisStock(String roomId) {
//        // 回滚逻辑：因为MySQL扣减失败，需要恢复Redis中的预扣库存
//        // 注意：这里需要原子操作，最好也用Lua脚本
//        log.warn("需要回滚Redis库存: roomId={}", roomId);
//    }
//}

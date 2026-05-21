package com.myy.weitutravel.flashSale.service;//package com.myy.shop.book.service;
//
//import com.myy.shop.book.vo.BookingRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.spring.core.RocketMQTemplate;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.stereotype.Service;
//
///**
// * 第二层防线：消息队列异步削峰
// * 将抢购成功的请求异步处理，保护数据库
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class MqConsumerService {
//
//    private final RocketMQTemplate rocketMQTemplate;
//
//    @Value("${rocketmq.producer.group}")
//    private String producerGroup;
//
//    private static final String ORDER_TOPIC = "hotel_order_topic";
//    private static final String ORDER_TAG = "create_order";
//
//    /**
//     * 发送订单创建消息
//     */
//    public void sendOrderMessage(BookingRequest request, String userId) {
//        String orderMessage = String.format("{\"roomId\":%d,\"userId\":%d,\"timestamp\":%d}",
//                request.getRoomId(), userId, System.currentTimeMillis());
//
//        rocketMQTemplate.syncSend(
//                ORDER_TOPIC + ":" + ORDER_TAG,
//                MessageBuilder.withPayload(orderMessage).build()
//        );
//
//        log.info("订单消息已发送: roomId={}, userId={}", request.getRoomId(), userId);
//    }
//}
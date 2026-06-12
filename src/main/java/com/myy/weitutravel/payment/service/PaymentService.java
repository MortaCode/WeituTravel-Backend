package com.myy.weitutravel.payment.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myy.weitutravel.cart.entity.CartItem;
import com.myy.weitutravel.cart.mapper.CartItemMapper;
import com.myy.weitutravel.common.exception.BizException;
import com.myy.weitutravel.order.entity.Order;
import com.myy.weitutravel.order.entity.OrderItem;
import com.myy.weitutravel.order.mapper.OrderItemMapper;
import com.myy.weitutravel.order.mapper.OrderMapper;
import com.myy.weitutravel.payment.vo.OrderSubmitVo;
import com.myy.weitutravel.product.entity.Product;
import com.myy.weitutravel.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    /**
     * 从购物车选中项创建订单并扣库存。
     * @return 订单对象（status=0 待支付）
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(String userId, OrderSubmitVo vo) {
        // 1. 获取已选中的购物车项
        List<CartItem> items = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getSelected, 1));

        if (items.isEmpty()) {
            throw new BizException("请选择要结算的商品");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 2. 校验库存并计算总价
        for (CartItem item : items) {
            Product p = productMapper.selectById(item.getProductId());
            if (p == null || p.getStatus() == 0) {
                throw new BizException("商品【" + item.getProductId() + "】已下架，请重新选择");
            }
            if (item.getQuantity() > p.getStock()) {
                throw new BizException("商品【" + p.getName() + "】库存不足，当前库存: " + p.getStock());
            }
            totalAmount = totalAmount.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 3. 创建订单
        String orderId = IdUtil.fastSimpleUUID();
        Order order = new Order();
        order.setId(IdUtil.fastSimpleUUID());
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setAmount(totalAmount);
        order.setStatus(0); // 待支付
        order.setVersion(0);
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        order.setDelFlag(0);
        orderMapper.insert(order);

        // 4. 创建订单商品快照 + 扣库存
        for (CartItem item : items) {
            Product p = productMapper.selectById(item.getProductId());

            OrderItem oi = new OrderItem();
            oi.setId(IdUtil.fastSimpleUUID());
            oi.setOrderId(orderId);
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setProductImage(p.getImage());
            oi.setPrice(p.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setAmount(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(oi);

            // 乐观锁扣库存
            int rows = productMapper.deductStock(p.getId(), item.getQuantity());
            if (rows == 0) {
                throw new BizException("商品【" + p.getName() + "】库存扣减失败");
            }
        }

        // 5. 清空已购买购物车项
        cartItemMapper.deleteSelectedByUserId(userId);

        log.info("订单创建成功: orderId={}, userId={}, amount={}, items={}", orderId, userId, totalAmount, items.size());
        return order;
    }

    /**
     * 支付成功回调，更新订单状态。
     * channelOrderNo: 第三方支付流水号
     */
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(String orderId, String channel, String channelOrderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
        if (order == null) {
            log.error("支付回调订单不存在: orderId={}", orderId);
            return;
        }
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            log.warn("订单状态异常，忽略回调: orderId={}, status={}", orderId, order.getStatus());
            return;
        }
        order.setStatus(2); // 支付成功
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("支付成功: orderId={}, channel={}, channelOrderNo={}", orderId, channel, channelOrderNo);
    }

    public Order getOrder(String orderId, String userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderId, orderId)
                        .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    public List<OrderItem> getOrderItems(String orderId) {
        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }
}

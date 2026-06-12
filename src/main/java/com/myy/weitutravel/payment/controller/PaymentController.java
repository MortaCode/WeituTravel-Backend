package com.myy.weitutravel.payment.controller;

import com.myy.weitutravel.common.api.Result;
import com.myy.weitutravel.login.entity.User;
import com.myy.weitutravel.login.service.UserService;
import com.myy.weitutravel.order.entity.Order;
import com.myy.weitutravel.order.entity.OrderItem;
import com.myy.weitutravel.payment.service.PaymentService;
import com.myy.weitutravel.payment.vo.OrderSubmitVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    /**
     * 结算：从购物车选中项创建订单
     */
    @PostMapping("submit")
    public Result<Order> submit(@Validated @RequestBody OrderSubmitVo vo, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return Result.success(paymentService.createOrder(user.getId(), vo));
    }

    /**
     * 查询订单详情
     */
    @GetMapping("order/{orderId}")
    public Result<Order> getOrder(@PathVariable String orderId, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return Result.success(paymentService.getOrder(orderId, user.getId()));
    }

    /**
     * 查询订单商品明细
     */
    @GetMapping("order/{orderId}/items")
    public Result<List<OrderItem>> getOrderItems(@PathVariable String orderId) {
        return Result.success(paymentService.getOrderItems(orderId));
    }

    /**
     * 支付回调（模拟 / 实际对接微信支付后使用）
     */
    @PostMapping("callback")
    public Result<String> callback(@RequestParam String orderId,
                                    @RequestParam String channel,
                                    @RequestParam String channelOrderNo) {
        paymentService.paySuccess(orderId, channel, channelOrderNo);
        return Result.success("OK");
    }
}

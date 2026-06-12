package com.myy.weitutravel.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("t_order_item")
@Data
public class OrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String orderId;

    private String productId;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal amount;

    private LocalDateTime createTime;
}

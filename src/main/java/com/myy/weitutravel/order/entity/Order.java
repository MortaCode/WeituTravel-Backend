package com.myy.weitutravel.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("t_order")
@Data
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String orderId;

    private String userId;

    private BigDecimal amount;

    /** 0-待支付 1-支付中 2-支付成功 3-支付失败 4-退款中 5-退款成功 6-已关闭 */
    private Integer status;

    private Integer version;

    private LocalDateTime payTime;

    private LocalDateTime expireTime;

    private Integer delFlag;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

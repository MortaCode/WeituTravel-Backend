package com.myy.weitutravel.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("t_cart_item")
@Data
public class CartItem {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    private String productId;

    private Integer quantity;

    private Integer selected;

    private LocalDateTime createTime;

    // ---- 非数据库字段，关联查询 ----
    private transient String productName;
    private transient String productImage;
    private transient java.math.BigDecimal productPrice;
}

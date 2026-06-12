package com.myy.weitutravel.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("t_product")
@Data
public class Product {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private String image;

    private String images;

    private String category;

    private Integer status;

    private Integer sales;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

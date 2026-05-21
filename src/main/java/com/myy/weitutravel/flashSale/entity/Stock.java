package com.myy.weitutravel.flashSale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_stock")
public class Stock {
    private String id;
    private String stockId;
    private Integer stock;
    private Integer version;
    private LocalDateTime updateTime;
}

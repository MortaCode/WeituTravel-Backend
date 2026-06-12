package com.myy.weitutravel.cart.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CartAddVo {

    @NotBlank(message = "商品ID不能为空")
    private String productId;

    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
}

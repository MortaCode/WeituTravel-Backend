package com.myy.weitutravel.payment.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderSubmitVo {

    @NotBlank(message = "支付渠道不能为空")
    private String channel;
}

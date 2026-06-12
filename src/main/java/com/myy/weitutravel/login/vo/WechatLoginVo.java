package com.myy.weitutravel.login.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WechatLoginVo {

    @NotBlank(message = "微信授权码不能为空")
    private String code;
}

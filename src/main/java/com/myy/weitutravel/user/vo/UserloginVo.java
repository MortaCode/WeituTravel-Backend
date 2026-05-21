package com.myy.weitutravel.user.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserloginVo {

    //用户ID
    @NotBlank(message = "用户不能为空")
    private String userId;

}

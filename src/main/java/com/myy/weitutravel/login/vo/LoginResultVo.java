package com.myy.weitutravel.login.vo;

import com.myy.weitutravel.login.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultVo {

    private boolean needBind;
    private String bindToken;
    private User user;
    private String token;
}

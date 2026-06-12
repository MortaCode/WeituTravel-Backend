package com.myy.weitutravel.login.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName(value = "t_user_wechat")
@Data
public class UserWechat {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    private String openid;

    private String unionid;

    private LocalDateTime createTime;
}

package com.myy.weitutravel.login.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName(value = "t_user")
@Data
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String mobile;

    private String nickname;

    private String avatar;

    private LocalDateTime createTime;
}

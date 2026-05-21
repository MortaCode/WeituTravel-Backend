package com.myy.weitutravel.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName(value ="t_user")
@Data
public class User {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     *
     */
    private String username;
}

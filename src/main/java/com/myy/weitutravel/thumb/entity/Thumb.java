package com.myy.weitutravel.thumb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 
 * @TableName thumb
 */
@TableName(value ="t_thumb")
@Data
public class Thumb {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private String id;

    /**
     *
     */
    private String userid;

    /**
     *
     */
    private String blogid;

    /**
     * 创建时间
     */
    private Date createtime;
}
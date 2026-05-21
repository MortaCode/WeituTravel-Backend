package com.myy.weitutravel.thumb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 
 * @TableName blog
 */
@TableName(value ="t_blog")
@Data
public class Blog {
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
     * 标题
     */
    private String title;

    /**
     * 封面
     */
    private String coverimg;

    /**
     * 内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Long thumbcount;

    /**
     * 创建时间
     */
    private Date createtime;

    /**
     * 更新时间
     */
    private Date updatetime;
}
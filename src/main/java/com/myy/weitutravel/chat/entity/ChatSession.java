package com.myy.weitutravel.chat.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 对话会话表
 * @TableName session
 */
@TableName(value ="t_session")
@Data
public class ChatSession {
    /**
     * 会话ID，会话表主键
     */
    @TableId
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 使用的模型名称
     */
    private String modelName;

    /**
     * 消息总数
     */
    private Integer messageCount;

    /**
     * 删除状态：0-未删除，1-删除
     */
    private Integer delFlag;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
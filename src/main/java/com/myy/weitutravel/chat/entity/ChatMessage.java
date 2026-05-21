package com.myy.weitutravel.chat.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 对话消息表
 * @TableName message
 */
@TableName(value ="t_message")
@Data
public class ChatMessage {
    /**
     * 消息ID，消息表主键
     */
    @TableId
    private String id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 角色：user/assistant/system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：text/image/file
     */
    private String messageType;

    /**
     * token数量
     */
    private Integer tokenCount;

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
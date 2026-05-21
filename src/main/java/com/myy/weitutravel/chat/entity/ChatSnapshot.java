package com.myy.weitutravel.chat.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 会话记忆快照表
 * @TableName snapshot
 */
@TableName(value ="t_snapshot")
@Data
public class ChatSnapshot {
    /**
     * 快照ID，快照表主键
     */
    @TableId
    private String id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 快照时的消息数量
     */
    private Integer messageCount;

    /**
     * 数据校验和
     */
    private String checksum;

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

    /**
     * 序列化的记忆数据
     */
    private byte[] snapshotData;
}
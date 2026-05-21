-- 创建数据库
CREATE DATABASE IF NOT EXISTS `travel`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `travel`;


-- 用户表
create table if not exists user
(
    id       varchar(32)
        primary key,
    username varchar(128) not null
);


-- 订单主表：状态机核心
CREATE TABLE `t_order` (
                           `id` VARCHAR(32) NOT NULL COMMENT '主键',
                           `order_id` VARCHAR(32) NOT NULL COMMENT '订单号（业务主键）',
                           `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
                           `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
                           `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0-待支付 1-支付中 2-支付成功 3-支付失败 4-退款中 5-退款成功 6-已关闭',
                           `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                           `pay_time` DATETIME DEFAULT NULL COMMENT '支付成功时间',
                           `expire_time` DATETIME NOT NULL COMMENT '订单过期时间',
                           `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
                           `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_order_id` (`order_id`),
                           KEY `idx_user_id` (`user_id`),
                           KEY `idx_status` (`status`),
                           KEY `idx_create_time` (`create_time`),
                           KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';


-- 支付记录表：记录每次支付尝试
CREATE TABLE `t_payment_record` (
                                    `id` VARCHAR(32) NOT NULL COMMENT '主键',
                                    `payment_no` VARCHAR(32) NOT NULL COMMENT '支付流水号',
                                    `order_id` VARCHAR(32) NOT NULL COMMENT '订单号',
                                    `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
                                    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
                                    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0-处理中 1-成功 2-失败 3-退款中 4-已退款',
                                    `channel` VARCHAR(20) NOT NULL COMMENT '支付渠道：WECHAT/ALIPAY',
                                    `channel_order_no` VARCHAR(64) DEFAULT NULL COMMENT '第三方支付订单号',
                                    `error_code` VARCHAR(32) DEFAULT NULL COMMENT '错误码',
                                    `error_msg` VARCHAR(256) DEFAULT NULL COMMENT '错误信息',
                                    `notify_time` DATETIME DEFAULT NULL COMMENT '回调通知时间',
                                    `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
                                    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_payment_no` (`payment_no`),
                                    KEY `idx_order_id` (`order_id`),
                                    KEY `idx_channel_order_no` (`channel_order_no`),
                                    KEY `idx_user_id` (`user_id`),
                                    KEY `idx_status` (`status`),
                                    KEY `idx_create_time` (`create_time`),
                                    KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';


-- 幂等控制表：防重放攻击
CREATE TABLE `t_idempotent_record` (
                                       `id` VARCHAR(32) NOT NULL COMMENT '主键',
                                       `idempotent_key` VARCHAR(128) NOT NULL COMMENT '幂等键（如：pay:orderId:userId）',
                                       `business_type` VARCHAR(32) NOT NULL COMMENT '业务类型：PAYMENT/REFUND',
                                       `business_id` VARCHAR(64) NOT NULL COMMENT '业务ID（订单号）',
                                       `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-处理中 1-成功 2-失败',
                                       `result_data` TEXT DEFAULT NULL COMMENT '处理结果JSON',
                                       `expire_time` DATETIME NOT NULL COMMENT '过期时间（24小时后可删除）',
                                       `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
                                       `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_idempotent_key` (`idempotent_key`),
                                       KEY `idx_business_id` (`business_id`),
                                       KEY `idx_expire_time` (`expire_time`),
                                       KEY `idx_del_flag` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幂等控制表';

-- 创建酒店库存表
CREATE TABLE `t_stock` (
                               `id` VARCHAR(32) NOT NULL COMMENT '主键ID',
                               `stock_id` VARCHAR(32) NOT NULL COMMENT '库存ID',
                               `stock` INT(11) NOT NULL DEFAULT 0 COMMENT '库存数量',
                               `version` INT(11) NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                               `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               PRIMARY KEY (`id`),
                               INDEX `idx_stock_id` (`stock_id`) COMMENT '库存ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';


-- 内容表
create table if not exists t_blog
(
    id         varchar(32)
        primary key,
    userId     varchar(32)                             not null,
    title      varchar(512)                       null comment '标题',
    coverImg   varchar(1024)                      null comment '封面',
    content    text                               not null comment '内容',
    thumbCount int      default 0                 not null comment '点赞数',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
);
create index idx_userId
    on t_blog (userId);

-- 点赞记录表
create table if not exists t_thumb
(
    id         varchar(32)
        primary key,
    userId     varchar(32)                            not null,
    blogId     varchar(32)                             not null,
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间'
);
create unique index idx_userId_blogId
    on t_thumb (userId, blogId);


-- 会话表
CREATE TABLE `t_session` (
                           `id` VARCHAR(32) NOT NULL COMMENT '会话ID，会话表主键',
                           `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
                           `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
                           `model_name` VARCHAR(50) NOT NULL COMMENT '使用的模型名称',
                           `message_count` INT DEFAULT 0 COMMENT '消息总数',
                           `del_flag` TINYINT DEFAULT 0 COMMENT '删除状态：0-未删除，1-删除',
                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           PRIMARY KEY (`id`),
                           KEY `idx_user_id` (`user_id`),
                           KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

-- 对话消息表
CREATE TABLE `t_message` (
                           `id` VARCHAR(32) NOT NULL COMMENT '消息ID，消息表主键',
                           `session_id` VARCHAR(32) NOT NULL COMMENT '会话ID',
                           `role` VARCHAR(20) NOT NULL COMMENT '角色：user/assistant/system',
                           `content` TEXT NOT NULL COMMENT '消息内容',
                           `message_type` VARCHAR(20) DEFAULT 'text' COMMENT '消息类型：text/image/file',
                           `token_count` INT DEFAULT 0 COMMENT 'token数量',
                           `del_flag` TINYINT DEFAULT 0 COMMENT '删除状态：0-未删除，1-删除',
                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           PRIMARY KEY (`id`),
                           KEY `idx_session_id` (`session_id`),
                           KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

-- 会话记忆快照表（用于快速恢复）
CREATE TABLE `t_snapshot` (
                            `id` VARCHAR(32) NOT NULL COMMENT '快照ID，快照表主键',
                            `session_id` VARCHAR(32) NOT NULL COMMENT '会话ID',
                            `snapshot_data` LONGBLOB NOT NULL COMMENT '序列化的记忆数据',
                            `message_count` INT NOT NULL COMMENT '快照时的消息数量',
                            `checksum` VARCHAR(64) DEFAULT NULL COMMENT '数据校验和',
                            `del_flag` TINYINT DEFAULT 0 COMMENT '删除状态：0-未删除，1-删除',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            KEY `idx_session_id` (`session_id`),
                            KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话记忆快照表';
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `travel`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `travel`;

-- 用户表：手机号作为系统唯一用户标识
CREATE TABLE IF NOT EXISTS `t_user` (
       `id`          VARCHAR(32)  NOT NULL COMMENT '用户ID',
       `mobile`      VARCHAR(20)  NOT NULL COMMENT '手机号（唯一标识）',
       `nickname`    VARCHAR(128) DEFAULT NULL COMMENT '昵称',
       `avatar`      VARCHAR(512) DEFAULT NULL COMMENT '头像',
       `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
       PRIMARY KEY (`id`),
       UNIQUE KEY `uk_mobile` (`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 微信绑定表：记录微信OpenId与用户的绑定关系
CREATE TABLE IF NOT EXISTS `t_user_wechat` (
       `id`          VARCHAR(32)  NOT NULL COMMENT '主键',
       `user_id`     VARCHAR(32)  NOT NULL COMMENT '用户ID',
       `openid`      VARCHAR(128) NOT NULL COMMENT '微信OpenId',
       `unionid`     VARCHAR(128) DEFAULT NULL COMMENT '微信UnionId',
       `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
       PRIMARY KEY (`id`),
       UNIQUE KEY `uk_openid` (`openid`),
       KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信绑定表';
-- ============================================================
-- 商品模块
-- ============================================================

-- 商品表
CREATE TABLE IF NOT EXISTS `t_product` (
                                           `id`          VARCHAR(32)    NOT NULL COMMENT '商品ID',
                                           `name`        VARCHAR(200)   NOT NULL COMMENT '商品名称',
                                           `description` TEXT           DEFAULT NULL COMMENT '商品描述',
                                           `price`       DECIMAL(10,2)  NOT NULL COMMENT '价格',
                                           `stock`       INT            NOT NULL DEFAULT 0 COMMENT '库存',
                                           `image`       VARCHAR(512)   DEFAULT NULL COMMENT '主图',
                                           `images`      TEXT           DEFAULT NULL COMMENT '轮播图（JSON数组）',
                                           `category`    VARCHAR(64)    DEFAULT NULL COMMENT '分类',
                                           `status`      TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：0-下架 1-上架',
                                           `sales`       INT            NOT NULL DEFAULT 0 COMMENT '销量',
                                           `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           PRIMARY KEY (`id`),
                                           KEY `idx_category` (`category`),
                                           KEY `idx_status` (`status`),
                                           KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 购物车表
CREATE TABLE IF NOT EXISTS `t_cart_item` (
                                             `id`          VARCHAR(32) NOT NULL COMMENT '主键',
                                             `user_id`     VARCHAR(32) NOT NULL COMMENT '用户ID',
                                             `product_id`  VARCHAR(32) NOT NULL COMMENT '商品ID',
                                             `quantity`    INT         NOT NULL DEFAULT 1 COMMENT '数量',
                                             `selected`    TINYINT     NOT NULL DEFAULT 1 COMMENT '是否选中：0-未选 1-选中',
                                             `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             PRIMARY KEY (`id`),
                                             KEY `idx_user_id` (`user_id`),
                                             UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

-- 订单商品快照表
CREATE TABLE IF NOT EXISTS `t_order_item` (
                                              `id`            VARCHAR(32)   NOT NULL COMMENT '主键',
                                              `order_id`      VARCHAR(32)   NOT NULL COMMENT '订单ID',
                                              `product_id`    VARCHAR(32)   NOT NULL COMMENT '商品ID',
                                              `product_name`  VARCHAR(200)  NOT NULL COMMENT '商品名称（快照）',
                                              `product_image` VARCHAR(512)  DEFAULT NULL COMMENT '商品图片（快照）',
                                              `price`         DECIMAL(10,2) NOT NULL COMMENT '下单时单价',
                                              `quantity`      INT           NOT NULL COMMENT '数量',
                                              `amount`        DECIMAL(10,2) NOT NULL COMMENT '小计',
                                              `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                              PRIMARY KEY (`id`),
                                              KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品快照表';
-- RestoFlow 建表脚本
-- 依据：docs/database-design.md
-- 执行：mysql -uroot -p restoflow < sql/schema.sql

SET NAMES utf8mb4;

-- ============================================================
-- 用户
-- ============================================================
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`   VARCHAR(50)  NOT NULL COMMENT '工号',
  `real_name`  VARCHAR(50)  NOT NULL COMMENT '姓名',
  `password`   VARCHAR(100) NOT NULL COMMENT '密码，BCrypt 哈希',
  `role`       VARCHAR(20)  NOT NULL COMMENT '角色',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户';

-- ============================================================
-- 桌台区域
-- ============================================================
CREATE TABLE `dining_area` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`       VARCHAR(50) NOT NULL COMMENT '区域名称',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='桌台区域';

-- ============================================================
-- 桌台
-- ============================================================
CREATE TABLE `dining_table` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `area_id`    BIGINT      NOT NULL COMMENT '所属区域',
  `name`       VARCHAR(50) NOT NULL COMMENT '桌台名',
  `seats`      INT         NOT NULL COMMENT '座位数',
  `status`     TINYINT     NOT NULL DEFAULT 1 COMMENT '1空台 2待下单 3待结账 4已结账',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_area` (`area_id`),
  CONSTRAINT `fk_table_area` FOREIGN KEY (`area_id`) REFERENCES `dining_area` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='桌台';

-- ============================================================
-- 菜品分类
-- ============================================================
CREATE TABLE `dish_category` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`       VARCHAR(50) NOT NULL COMMENT '分类名称',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品分类';

-- ============================================================
-- 菜品
-- ============================================================
CREATE TABLE `dish` (
  `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` BIGINT         NOT NULL COMMENT '所属分类',
  `name`        VARCHAR(50)    NOT NULL COMMENT '菜品名称',
  `price`       DECIMAL(10,2)  NOT NULL COMMENT '单价',
  `sold_out`    TINYINT        NOT NULL DEFAULT 0 COMMENT '0在售 1沽清',
  `created_at`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  CONSTRAINT `fk_dish_category` FOREIGN KEY (`category_id`) REFERENCES `dish_category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品';

-- ============================================================
-- 订单
-- ============================================================
CREATE TABLE `orders` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`         VARCHAR(20)   NOT NULL COMMENT '订单号',
  `table_id`         BIGINT        NOT NULL COMMENT '归属桌台（无外键，桌台可删除）',
  `table_name`       VARCHAR(50)   NOT NULL COMMENT '开台时桌台名快照',
  `people_count`     INT           NOT NULL COMMENT '用餐人数',
  `status`           TINYINT       NOT NULL COMMENT '1待下单 2待结账 3已结账',
  `original_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '菜品价格合计',
  `discount_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  `round_off_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '抹零金额',
  `payable_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '应收金额',
  `received_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实收金额',
  `opened_by`        BIGINT        NOT NULL COMMENT '开台操作人',
  `settled_by`       BIGINT        NULL     COMMENT '结账操作人',
  `settled_at`       DATETIME      NULL     COMMENT '结账时间',
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开台时间',
  `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_table` (`table_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_orders_opened_by`  FOREIGN KEY (`opened_by`)  REFERENCES `user` (`id`),
  CONSTRAINT `fk_orders_settled_by` FOREIGN KEY (`settled_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单';

-- ============================================================
-- 订单明细
-- ============================================================
CREATE TABLE `order_item` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`          BIGINT        NOT NULL COMMENT '归属订单',
  `dish_id`           BIGINT        NULL     COMMENT '菜品 id（无外键，菜品可删除）',
  `dish_name`         VARCHAR(50)   NOT NULL COMMENT '下单时菜名快照',
  `price`             DECIMAL(10,2) NOT NULL COMMENT '下单时单价快照',
  `quantity`          INT           NOT NULL COMMENT '下单数量',
  `refunded_quantity` INT           NOT NULL DEFAULT 0 COMMENT '已退数量',
  `refund_reason`     VARCHAR(200)  NULL     COMMENT '退菜原因',
  `refunded_by`       BIGINT        NULL     COMMENT '退菜操作人',
  `refunded_at`       DATETIME      NULL     COMMENT '退菜时间',
  `remark`            VARCHAR(100)  NULL     COMMENT '备注',
  `status`            TINYINT       NOT NULL COMMENT '1待制作 2已出餐 3已退',
  `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  CONSTRAINT `fk_item_order`     FOREIGN KEY (`order_id`)    REFERENCES `orders` (`id`),
  CONSTRAINT `fk_item_refunded`  FOREIGN KEY (`refunded_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细';

-- ============================================================
-- 支付记录
-- ============================================================
CREATE TABLE `payment` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    BIGINT        NOT NULL COMMENT '归属订单',
  `pay_method`  TINYINT       NOT NULL COMMENT '1现金 2支付宝',
  `amount`      DECIMAL(10,2) NOT NULL COMMENT '收款金额',
  `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '1有效 0失效',
  `operator_id` BIGINT        NOT NULL COMMENT '收款操作人',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收款时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_payment_order`    FOREIGN KEY (`order_id`)    REFERENCES `orders` (`id`),
  CONSTRAINT `fk_payment_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录';

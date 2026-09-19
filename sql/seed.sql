-- RestoFlow 初始数据
-- 执行：mysql -uroot -p restoflow < sql/seed.sql
-- 可重复执行：先清空业务数据再写入

SET NAMES utf8mb4;

-- 按外键依赖倒序清空
DELETE FROM `payment`;
DELETE FROM `order_item`;
DELETE FROM `orders`;
DELETE FROM `dish`;
DELETE FROM `dish_category`;
DELETE FROM `dining_table`;
DELETE FROM `dining_area`;
DELETE FROM `user`;

-- ============================================================
-- 用户
-- ============================================================
INSERT INTO `user` (`id`, `username`, `real_name`, `password`, `role`, `status`) VALUES
(1, 'admin', '管理员', '$2b$10$J3FsKxwcPtnQCDPH4rtL9u8NYKICJfEqsLG3VtmVdcjg37Y9MJc.u', 'admin', 1);

-- ============================================================
-- 桌台区域
-- ============================================================
INSERT INTO `dining_area` (`id`, `name`) VALUES
(1, 'A区'),
(2, '包房');

-- ============================================================
-- 桌台
-- ============================================================
INSERT INTO `dining_table` (`id`, `area_id`, `name`, `seats`, `status`) VALUES
(1, 1, 'A1', 4, 1),
(2, 1, 'A2', 4, 1),
(3, 1, 'A3', 4, 1),
(4, 2, '包1', 6, 1),
(5, 2, '包2', 6, 1),
(6, 2, '包3', 8, 1);

-- ============================================================
-- 菜品分类
-- ============================================================
INSERT INTO `dish_category` (`id`, `name`) VALUES
(1, '凉菜'),
(2, '热菜'),
(3, '主食');

-- ============================================================
-- 菜品
-- ============================================================
INSERT INTO `dish` (`id`, `category_id`, `name`, `price`, `sold_out`) VALUES
(1,  1, '拍黄瓜',     12.00, 0),
(2,  1, '口水鸡',     28.00, 0),
(3,  2, '宫保鸡丁',   38.00, 0),
(4,  2, '水煮鱼',     68.00, 0),
(5,  2, '麻婆豆腐',   22.00, 0),
(6,  2, '回锅肉',     36.00, 0),
(7,  2, '干煸四季豆', 26.00, 0),
(8,  3, '米饭',        2.00, 0),
(9,  3, '蛋炒饭',     16.00, 0),
(10, 3, '手工面',     14.00, 0);

-- ============================================================
-- 校验
-- ============================================================
SELECT '管理员' AS 项, COUNT(*) AS 数量 FROM `user`
UNION ALL SELECT '区域',   COUNT(*) FROM `dining_area`
UNION ALL SELECT '桌台',   COUNT(*) FROM `dining_table`
UNION ALL SELECT '分类',   COUNT(*) FROM `dish_category`
UNION ALL SELECT '菜品',   COUNT(*) FROM `dish`;

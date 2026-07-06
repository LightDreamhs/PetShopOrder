-- ============================================
-- PetShopOrder 联调测试种子数据（仅开发环境）
-- ============================================
-- 用途：本地 / 联调环境的示例商品、SKU、会员等级、会员及其手机号映射。
-- 生产环境【不加载】此文件 —— 商品与会员由 admin 后台手工录入。
--
-- 依赖：init.sql 已建表，且 system_config（id=1）已存在。
-- 加载方式：dev 的 backend/docker-compose.yml 同时挂载 init.sql 与本文件到
--           /docker-entrypoint-initdb.d/，并用 01-/02- 数字前缀重命名挂载目标，
--           确保 schema（init.sql）先于本文件执行。生产 docker-compose.prod.yml 仅挂载 init.sql。

SET NAMES utf8mb4;

-- 商品 + SKU
INSERT INTO product (id, name, type, service_category, status, support_delivery, sort) VALUES
(1,  '皇家金毛成犬粮',     'GOODS',   NULL,           'ON_SALE', 1, 1),
(2,  '伯纳天纯中大型犬粮', 'GOODS',   NULL,           'ON_SALE', 1, 2),
(3,  '皇家英短成猫粮',     'GOODS',   NULL,           'ON_SALE', 1, 1),
(4,  '渴望鸡肉猫粮',       'GOODS',   NULL,           'ON_SALE', 1, 2),
(5,  '疯狂小狗鸡肉干',     'GOODS',   NULL,           'ON_SALE', 1, 1),
(6,  '猫条零食',           'GOODS',   NULL,           'ON_SALE', 1, 2),
(7,  '宠物尿垫',           'GOODS',   NULL,           'ON_SALE', 1, 1),
(8,  '宠物牵引绳',         'GOODS',   NULL,           'ON_SALE', 1, 2),
(9,  '洗澡服务',           'SERVICE', 'MAIN_SERVICE', 'ON_SALE', 0, 1),
(10, '药浴服务',           'SERVICE', 'MAIN_SERVICE', 'ON_SALE', 0, 2),
(11, '美容修剪',           'SERVICE', 'MAIN_SERVICE', 'ON_SALE', 0, 1),
(12, '猫洗澡+修剪',        'SERVICE', 'MAIN_SERVICE', 'ON_SALE', 0, 2);

INSERT INTO sku (product_id, spec_name, price, member_price, duration, stock, sort) VALUES
-- 狗粮
(1,  '15kg',  '368.00', '298.00', NULL, -1, 1),
(1,  '3kg',   '89.00',  '72.00',  NULL, -1, 2),
(2,  '10kg',  '199.00', '168.00', NULL, -1, 1),
-- 猫粮
(3,  '10kg',  '329.00', '269.00', NULL, -1, 1),
(3,  '2kg',   '79.00',  '65.00',  NULL, -1, 2),
(4,  '5.4kg', '469.00', '399.00', NULL, -1, 1),
(4,  '1.8kg', '169.00', '145.00', NULL, -1, 2),
-- 零食
(5,  '500g',  '39.90',  '32.90',  NULL, -1, 1),
(6,  '15支装', '25.00',  '19.90',  NULL, -1, 1),
(6,  '30支装', '45.00',  '36.00',  NULL, -1, 2),
-- 用品
(7,  '60×45cm 50片', '35.00', '28.00', NULL, -1, 1),
(7,  '60×60cm 30片', '32.00', '26.00', NULL, -1, 2),
(8,  'S号（小型犬）', '29.00', '24.00', NULL, -1, 1),
(8,  'M号（中型犬）', '35.00', '28.00', NULL, -1, 2),
(8,  'L号（大型犬）', '42.00', '35.00', NULL, -1, 3),
-- 洗护（duration 单位：分钟）
(9,  '小型犬（<10kg）',  '79.00',  NULL, 60,  -1, 1),
(9,  '中型犬（10-25kg）', '99.00', NULL, 75,  -1, 2),
(9,  '大型犬（>25kg）',  '129.00', NULL, 90,  -1, 3),
(10, '小型犬',   '119.00', NULL, 75,  -1, 1),
(10, '中大型犬', '159.00', NULL, 90,  -1, 2),
-- 美容
(11, '基础护理', '139.00', NULL, 90,  -1, 1),
(11, '全套精修', '199.00', NULL, 110, -1, 2),
(12, '短毛猫',   '129.00', NULL, 60,  -1, 1),
(12, '长毛猫',   '169.00', NULL, 75,  -1, 2);

-- 会员等级
INSERT INTO member_level (id, name, discount_rate, sort, status) VALUES
(1, '500档会员',  0.9000, 10, 1),
(2, '1000档会员', 0.8500, 20, 1),
(3, '2000档会员', 0.8000, 30, 1),
(4, '5000档会员', 0.7000, 40, 1);

-- 会员 + 手机号映射
INSERT INTO member (id, name, level_id) VALUES
(1, '张伟', 3),
(2, '李娜', 2),
(3, '王强', 1),
(4, '赵敏', 2),
(5, '陈刚', 1),
(6, '刘芳', 4);

INSERT INTO member_phone (member_id, phone) VALUES
(1, '13800001111'),
(1, '13800002222'),
(2, '13900003333'),
(3, '13700004444'),
(3, '13700005555'),
(4, '13600006666'),
(5, '13500007777'),
(6, '13400008888'),
(6, '13400009999');

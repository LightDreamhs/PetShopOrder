-- ============================================
-- PetShopOrder 数据库初始化脚本
-- MySQL 8.0+ / UTF-8MB4
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 商品体系
-- ============================================

-- 商品/服务（统一表，type 区分）
CREATE TABLE IF NOT EXISTS product (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(128) NOT NULL,
    description      TEXT         NULL,
    cover_img        VARCHAR(255) NULL,
    type             VARCHAR(16)  NOT NULL COMMENT 'GOODS / SERVICE',
    status           VARCHAR(16)  NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE / OFF_SALE',
    support_delivery TINYINT      NOT NULL DEFAULT 0 COMMENT '仅 GOODS 有效，SERVICE 强制为 0',
    sort             INT          NOT NULL DEFAULT 0,
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_type_status (type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品/服务';

-- SKU（多规格）
CREATE TABLE IF NOT EXISTS sku (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT        NOT NULL,
    spec_name    VARCHAR(64)   NOT NULL COMMENT '如 "5kg" / "中型犬（10-25kg）"',
    price        DECIMAL(10,2) NOT NULL COMMENT '原价',
    member_price DECIMAL(10,2) NULL COMMENT '会员价（仅 GOODS 用，SERVICE 忽略）',
    stock        INT           NOT NULL DEFAULT 0 COMMENT 'SERVICE 可为 -1 表示不限',
    sort         INT           NOT NULL DEFAULT 0,
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sku_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SKU';

-- ============================================
-- 会员体系
-- ============================================

-- C 端用户账号（手机号验证码登录）
CREATE TABLE IF NOT EXISTS app_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone           VARCHAR(16) NOT NULL COMMENT '登录手机号',
    status          TINYINT     NOT NULL DEFAULT 1 COMMENT '0 禁用 / 1 正常',
    last_login_time DATETIME    NULL,
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='C端用户';

-- 会员等级
CREATE TABLE IF NOT EXISTS member_level (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(32)  NOT NULL COMMENT '如 "500档会员"',
    discount_rate DECIMAL(5,4) NOT NULL COMMENT '服务折扣率，0.85 = 8.5折',
    sort          INT          NOT NULL DEFAULT 0,
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0 停用 / 1 启用',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级';

-- 会员
CREATE TABLE IF NOT EXISTS member (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(32)  NOT NULL,
    level_id    BIGINT       NOT NULL,
    remark      VARCHAR(255) NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_member_level (level_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员';

-- 会员手机号映射（一个会员可绑定多个手机号）
CREATE TABLE IF NOT EXISTS member_phone (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT      NOT NULL,
    phone       VARCHAR(16) NOT NULL COMMENT '与 app_user.phone 对应',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_member_phone (phone),
    INDEX idx_member_phone_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员手机号映射';

-- ============================================
-- 订单
-- ============================================

-- 订单主表
CREATE TABLE IF NOT EXISTS orders (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no                VARCHAR(32)    NOT NULL COMMENT '日期+雪花算法',
    user_id                 BIGINT         NOT NULL COMMENT 'app_user.id',
    customer_phone_snapshot VARCHAR(16)    NOT NULL COMMENT '下单时手机号快照',
    customer_name           VARCHAR(32)    NULL COMMENT '联系人',
    member_id               BIGINT         NULL COMMENT '命中会员则记录',
    member_level_snapshot   VARCHAR(32)    NULL COMMENT '下单时等级名快照',
    goods_amount            DECIMAL(10,2)  NOT NULL COMMENT '用品金额（已折扣）',
    service_amount          DECIMAL(10,2)  NOT NULL COMMENT '服务金额（已折扣）',
    delivery_fee            DECIMAL(10,2)  NOT NULL DEFAULT 0.00 COMMENT '配送费',
    total_amount            DECIMAL(10,2)  NOT NULL COMMENT 'goods + service + delivery_fee',
    need_delivery           TINYINT        NOT NULL DEFAULT 0,
    delivery_address        VARCHAR(255)   NULL,
    delivery_lat            DECIMAL(10,7)  NULL,
    delivery_lng            DECIMAL(10,7)  NULL,
    delivery_distance       INT            NULL COMMENT '与店铺直线距离，单位米',
    processed               TINYINT        NOT NULL DEFAULT 0 COMMENT '0 未处理 / 1 已处理',
    remark                  VARCHAR(255)   NULL,
    create_time             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_order_no (order_no),
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_member (member_id),
    INDEX idx_orders_create_time (create_time),
    INDEX idx_orders_processed (processed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单';

-- 订单明细
CREATE TABLE IF NOT EXISTS order_item (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id       BIGINT        NOT NULL,
    product_id     BIGINT        NOT NULL,
    sku_id         BIGINT        NULL,
    type           VARCHAR(16)   NOT NULL COMMENT 'GOODS / SERVICE',
    product_name   VARCHAR(128)  NOT NULL COMMENT '下单时快照',
    sku_name       VARCHAR(64)   NULL COMMENT '下单时快照',
    original_price DECIMAL(10,2) NOT NULL COMMENT '原价快照',
    deal_price     DECIMAL(10,2) NOT NULL COMMENT '成交价快照',
    quantity       INT           NOT NULL,
    subtotal       DECIMAL(10,2) NOT NULL COMMENT 'deal_price × quantity',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_item_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细';

-- ============================================
-- 系统
-- ============================================

-- 后台账号
CREATE TABLE IF NOT EXISTS admin_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(32)   NOT NULL,
    password_hash   VARCHAR(255)  NOT NULL,
    real_name       VARCHAR(32)   NOT NULL,
    role            VARCHAR(16)   NOT NULL COMMENT 'BOSS / MANAGER / STAFF',
    status          TINYINT       NOT NULL DEFAULT 1 COMMENT '0 禁用 / 1 启用',
    last_login_time DATETIME      NULL,
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台账号';

-- 操作日志
CREATE TABLE IF NOT EXISTS operation_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    action      VARCHAR(32)  NOT NULL COMMENT '如 UPDATE_PRODUCT / UPDATE_CONFIG',
    target      VARCHAR(128) NOT NULL COMMENT '如 "商品:金毛粮5kg"',
    before_val  TEXT         NULL COMMENT '修改前值 JSON',
    after_val   TEXT         NULL COMMENT '修改后值 JSON',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_time (create_time),
    INDEX idx_log_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- 系统配置（单行表，ID=1）
CREATE TABLE IF NOT EXISTS system_config (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_lat             DECIMAL(10,7)  NULL COMMENT '店铺纬度',
    shop_lng             DECIMAL(10,7)  NULL COMMENT '店铺经度',
    delivery_radius_km   DECIMAL(6,2)   NOT NULL DEFAULT 5.00 COMMENT '配送半径（km）',
    delivery_min_amount  DECIMAL(10,2)  NOT NULL DEFAULT 20.00 COMMENT '起送价',
    delivery_fee_type    VARCHAR(16)    NOT NULL DEFAULT 'FREE' COMMENT 'FREE / TIERED',
    fixed_delivery_fee   DECIMAL(10,2)  NOT NULL DEFAULT 0.00 COMMENT '保留字段（当前不使用）',
    order_time_enabled   TINYINT        NOT NULL DEFAULT 0 COMMENT '预留：接单时段开关',
    order_start_time     TIME           NULL COMMENT '预留：接单开始时间',
    order_end_time       TIME           NULL COMMENT '预留：接单结束时间',
    qywx_webhook_url_enc VARBINARY(1024) NULL COMMENT '企微 Webhook URL 加密存储',
    has_qywx_webhook     TINYINT        NOT NULL DEFAULT 0 COMMENT '是否已配置 Webhook',
    payment_qr_url       VARCHAR(255)   NULL COMMENT '收款二维码图片地址',
    updated_by           BIGINT         NULL COMMENT 'admin_user.id',
    create_time          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

-- 分段运费配置
CREATE TABLE IF NOT EXISTS system_config_delivery_tier (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id       BIGINT       NOT NULL,
    min_distance_km DECIMAL(6,2) NOT NULL,
    max_distance_km DECIMAL(6,2) NOT NULL,
    fee             DECIMAL(10,2) NOT NULL,
    sort            INT          NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tier_config (config_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分段运费配置';

-- 系统配置变更日志
CREATE TABLE IF NOT EXISTS system_config_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id     BIGINT      NOT NULL,
    operator_id   BIGINT      NOT NULL COMMENT 'admin_user.id',
    operator_name VARCHAR(32) NOT NULL,
    summary       VARCHAR(255) NOT NULL,
    before_val    JSON        NULL,
    after_val     JSON        NULL,
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_log_config (config_id),
    INDEX idx_config_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置变更日志';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 初始数据（联调用）
-- ============================================

-- 系统配置（单行，ID=1）
INSERT INTO system_config (id, shop_lat, shop_lng, delivery_radius_km, delivery_min_amount, delivery_fee_type, fixed_delivery_fee, order_time_enabled, order_start_time, order_end_time)
VALUES (1, 31.2304000, 121.4737000, 6.00, 30.00, 'TIERED', 0.00, 1, '09:00:00', '21:00:00');

-- 分段运费
INSERT INTO system_config_delivery_tier (config_id, min_distance_km, max_distance_km, fee, sort) VALUES
(1, 0.00, 2.00, 3.00, 1),
(1, 2.00, 4.00, 5.00, 2),
(1, 4.00, 6.00, 8.00, 3);

-- 商品 + SKU
INSERT INTO product (id, name, type, status, support_delivery, sort) VALUES
(1,  '皇家金毛成犬粮',     'GOODS',   'ON_SALE', 1, 1),
(2,  '伯纳天纯中大型犬粮', 'GOODS',   'ON_SALE', 1, 2),
(3,  '皇家英短成猫粮',     'GOODS',   'ON_SALE', 1, 1),
(4,  '渴望鸡肉猫粮',       'GOODS',   'ON_SALE', 1, 2),
(5,  '疯狂小狗鸡肉干',     'GOODS',   'ON_SALE', 1, 1),
(6,  '猫条零食',           'GOODS',   'ON_SALE', 1, 2),
(7,  '宠物尿垫',           'GOODS',   'ON_SALE', 1, 1),
(8,  '宠物牵引绳',         'GOODS',   'ON_SALE', 1, 2),
(9,  '洗澡服务',           'SERVICE', 'ON_SALE', 0, 1),
(10, '药浴服务',           'SERVICE', 'ON_SALE', 0, 2),
(11, '美容修剪',           'SERVICE', 'ON_SALE', 0, 1),
(12, '猫洗澡+修剪',        'SERVICE', 'ON_SALE', 0, 2);

INSERT INTO sku (product_id, spec_name, price, member_price, stock, sort) VALUES
-- 狗粮
(1,  '15kg',  '368.00', '298.00', -1, 1),
(1,  '3kg',   '89.00',  '72.00',  -1, 2),
(2,  '10kg',  '199.00', '168.00', -1, 1),
-- 猫粮
(3,  '10kg',  '329.00', '269.00', -1, 1),
(3,  '2kg',   '79.00',  '65.00',  -1, 2),
(4,  '5.4kg', '469.00', '399.00', -1, 1),
(4,  '1.8kg', '169.00', '145.00', -1, 2),
-- 零食
(5,  '500g',  '39.90',  '32.90',  -1, 1),
(6,  '15支装', '25.00',  '19.90',  -1, 1),
(6,  '30支装', '45.00',  '36.00',  -1, 2),
-- 用品
(7,  '60×45cm 50片', '35.00', '28.00', -1, 1),
(7,  '60×60cm 30片', '32.00', '26.00', -1, 2),
(8,  'S号（小型犬）', '29.00', '24.00', -1, 1),
(8,  'M号（中型犬）', '35.00', '28.00', -1, 2),
(8,  'L号（大型犬）', '42.00', '35.00', -1, 3),
-- 洗护
(9,  '小型犬（<10kg）',  '79.00',  NULL, -1, 1),
(9,  '中型犬（10-25kg）', '99.00', NULL, -1, 2),
(9,  '大型犬（>25kg）',  '129.00', NULL, -1, 3),
(10, '小型犬',   '119.00', NULL, -1, 1),
(10, '中大型犬', '159.00', NULL, -1, 2),
-- 美容
(11, '基础护理', '139.00', NULL, -1, 1),
(11, '全套精修', '199.00', NULL, -1, 2),
(12, '短毛猫',   '129.00', NULL, -1, 1),
(12, '长毛猫',   '169.00', NULL, -1, 2);

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

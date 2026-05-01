-- ============================================
-- PetShopOrder 数据库初始化脚本
-- MySQL 8.0+ / UTF-8MB4
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 商品体系
-- ============================================

-- 商品分类
CREATE TABLE IF NOT EXISTS category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(32)  NOT NULL,
    icon        VARCHAR(255) NULL,
    type        VARCHAR(16)  NOT NULL COMMENT 'GOODS / SERVICE',
    sort        INT          NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类';

-- 商品/服务（统一表，type 区分）
CREATE TABLE IF NOT EXISTS product (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id      BIGINT       NOT NULL,
    name             VARCHAR(128) NOT NULL,
    description      TEXT         NULL,
    cover_img        VARCHAR(255) NULL,
    type             VARCHAR(16)  NOT NULL COMMENT 'GOODS / SERVICE',
    status           VARCHAR(16)  NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE / OFF_SALE',
    support_delivery TINYINT      NOT NULL DEFAULT 0 COMMENT '仅 GOODS 有效，SERVICE 强制为 0',
    sort             INT          NOT NULL DEFAULT 0,
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_category (category_id),
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
-- 初始数据
-- ============================================

-- 默认 BOSS 账号：由应用启动时自动初始化（admin / admin123）
-- 此处仅预留，实际 INSERT 由 DataInitializer 完成

-- 默认系统配置（单行，ID=1）
INSERT INTO system_config (id, shop_lat, shop_lng, delivery_radius_km, delivery_min_amount, delivery_fee_type, fixed_delivery_fee, order_time_enabled)
VALUES (1, NULL, NULL, 5.00, 20.00, 'FREE', 0.00, 0);

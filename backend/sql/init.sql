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
    service_category VARCHAR(16)  NULL COMMENT '服务子类：MAIN_SERVICE 主服务 / ADDON_SERVICE 附加服务。仅 type=SERVICE 时有效，GOODS 为 NULL',
    status           VARCHAR(16)  NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE / OFF_SALE',
    support_delivery TINYINT      NOT NULL DEFAULT 0 COMMENT '仅 GOODS 有效，SERVICE 强制为 0',
    sort             INT          NOT NULL DEFAULT 0,
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_type_status (type, status),
    INDEX idx_product_service_category (type, service_category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品/服务';

-- SKU（多规格）
CREATE TABLE IF NOT EXISTS sku (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT        NOT NULL,
    spec_name    VARCHAR(64)   NOT NULL COMMENT '如 "5kg" / "中型犬（10-25kg）"',
    price        DECIMAL(10,2) NOT NULL COMMENT '原价',
    member_price DECIMAL(10,2) NULL COMMENT '会员价（仅 GOODS 用，SERVICE 忽略）',
    duration     INT           NULL COMMENT '服务时长（分钟）。仅 SERVICE 的 SKU 用，GOODS 为 NULL',
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
    cancelled               TINYINT        NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 已取消（取消预约时联动置 1，商家后台据此识别）',
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

-- 主服务-附加服务绑定关系
CREATE TABLE IF NOT EXISTS main_service_addon (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    main_product_id  BIGINT NOT NULL COMMENT '主服务 product.id（service_category=MAIN_SERVICE）',
    addon_product_id BIGINT NOT NULL COMMENT '附加服务 product.id（service_category=ADDON_SERVICE）',
    sort             INT    NOT NULL DEFAULT 0,
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_main_addon (main_product_id, addon_product_id),
    INDEX idx_addon_main (addon_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主服务-附加服务绑定';

-- 服务预约记录（核心：与 orders 一对一，order_id 关联）
CREATE TABLE IF NOT EXISTS appointment (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT      NOT NULL COMMENT '关联 orders.id（一笔预约对应一个订单）',
    user_id         BIGINT      NOT NULL COMMENT 'app_user.id',
    main_product_id BIGINT      NOT NULL COMMENT '主服务 product.id 快照',
    main_sku_id     BIGINT      NOT NULL COMMENT '主服务 sku.id 快照',
    start_time      DATETIME    NOT NULL COMMENT '预约开始时间（顾客到店/服务开始）',
    end_time        DATETIME    NOT NULL COMMENT 'start_time + total_duration 分钟',
    total_duration  INT         NOT NULL COMMENT '总占用时长（分钟）= 主服务 + Σ附加服务',
    pet_info        VARCHAR(255) NULL COMMENT '宠物信息（文本：名字/种类/体重/性格等）',
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待服务 / SERVICED 已完成 / CANCELLED 已取消',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_appointment_order (order_id),
    INDEX idx_appointment_user (user_id),
    INDEX idx_appointment_status_time (status, start_time, end_time),
    INDEX idx_appointment_time_range (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务预约';

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
    order_time_enabled   TINYINT        NOT NULL DEFAULT 0 COMMENT '预约营业时段开关：1=限制预约开始时间在 order_start/end_time 内；实物商品下单不受此字段约束',
    order_start_time     TIME           NULL COMMENT '可预约开始时间下限（如 09:00）',
    order_end_time       TIME           NULL COMMENT '可预约开始时间上限（如 21:00，开区间）',
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

CREATE TABLE IF NOT EXISTS user_address (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL COMMENT 'app_user.id',
    label        VARCHAR(20)  NULL COMMENT '标签：家/公司/学校/其他',
    address      VARCHAR(255) NOT NULL COMMENT 'POI 名称或自定义地址',
    detail       VARCHAR(255) NULL COMMENT '补充详细地址（楼栋/门牌号）',
    lat          DECIMAL(10,7) NOT NULL,
    lng          DECIMAL(10,7) NOT NULL,
    is_default   TINYINT      NOT NULL DEFAULT 0 COMMENT '1=默认地址',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_address_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 系统默认配置（生产必需，admin 后台可改）
-- ============================================

-- 系统配置（单行，ID=1）
INSERT INTO system_config (id, shop_lat, shop_lng, delivery_radius_km, delivery_min_amount, delivery_fee_type, fixed_delivery_fee, order_time_enabled, order_start_time, order_end_time)
VALUES (1, 31.2304000, 121.4737000, 6.00, 30.00, 'TIERED', 0.00, 1, '09:00:00', '21:00:00');

-- 分段运费
INSERT INTO system_config_delivery_tier (config_id, min_distance_km, max_distance_km, fee, sort) VALUES
(1, 0.00, 2.00, 3.00, 1),
(1, 2.00, 4.00, 5.00, 2),
(1, 4.00, 6.00, 8.00, 3);

-- 测试种子数据（商品/SKU/会员等级/会员/手机号）已迁移到 init-seed-dev.sql，
-- 仅 dev 环境加载（dev compose 用 01/02 前缀同时挂载两文件）；生产 compose 仅挂载本文件，
-- 商品与会员由 admin 后台手工录入。

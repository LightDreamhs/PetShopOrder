-- ============================================
-- 多店改造迁移 v1（Phase 1 多店数据底座）
-- ============================================
-- 适用对象：已按旧版 init.sql 初始化的库（存在 system_config 单行表）。
-- 执行方式：docker exec 进 MySQL 容器执行（一次性脚本，勿重复执行）。
--   docker exec -i petorder-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" petshop_order' < migration_multi_store_v1.sql
-- 新库不需要本脚本：更新后的 init.sql 已直接建多店结构。
--
-- 内容：新增 shop / shop_config / shop_delivery_tier 三表；存量数据归入默认店 id=1；
--       业务表加 shop_id；sku 加店铺级 status；member_phone 唯一约束改为 (shop_id, phone)。
-- system_config / system_config_delivery_tier 保留一个版本周期，验证后另行下线。

SET NAMES utf8mb4;

-- 1. 门店表
CREATE TABLE IF NOT EXISTS shop (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(32)   NOT NULL COMMENT '店铺编码，二维码/URL 识别用',
    name          VARCHAR(64)   NOT NULL COMMENT '店铺名称',
    phone         VARCHAR(20)   NULL COMMENT '门店电话',
    address       VARCHAR(255)  NULL COMMENT '门店地址（展示用）',
    shop_lat      DECIMAL(10,7) NOT NULL COMMENT '纬度（配送中心）',
    shop_lng      DECIMAL(10,7) NOT NULL COMMENT '经度',
    status        VARCHAR(16)   NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN 营业 / CLOSED 歇业',
    sort          INT           NOT NULL DEFAULT 0,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_shop_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店';

-- 2. 店铺配置（每店一行，承接原 system_config；fixed_delivery_fee 已下线不迁移）
CREATE TABLE IF NOT EXISTS shop_config (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id              BIGINT        NOT NULL,
    shop_lat             DECIMAL(10,7) NULL COMMENT '店铺纬度',
    shop_lng             DECIMAL(10,7) NULL COMMENT '店铺经度',
    delivery_radius_km   DECIMAL(6,2)  NOT NULL DEFAULT 5.00 COMMENT '配送半径（km）',
    delivery_min_amount  DECIMAL(10,2) NOT NULL DEFAULT 20.00 COMMENT '起送价',
    delivery_fee_type    VARCHAR(16)   NOT NULL DEFAULT 'FREE' COMMENT 'FREE / TIERED',
    order_time_enabled   TINYINT       NOT NULL DEFAULT 0,
    order_start_time     TIME          NULL,
    order_end_time       TIME          NULL,
    qywx_webhook_url_enc VARBINARY(1024) NULL COMMENT '本店订单通知 Webhook（加密）',
    has_qywx_webhook     TINYINT       NOT NULL DEFAULT 0,
    payment_qr_url       VARCHAR(255)  NULL COMMENT '本店收款码',
    ad_enabled           TINYINT       NOT NULL DEFAULT 0,
    ad_image_url         VARCHAR(512)  NULL,
    ad_link_type         VARCHAR(16)   NULL,
    ad_link_target       VARCHAR(255)  NULL,
    updated_by           BIGINT        NULL,
    create_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_shop_config (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺配置';

-- 3. 店铺分段运费
CREATE TABLE IF NOT EXISTS shop_delivery_tier (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id         BIGINT        NOT NULL,
    min_distance_km DECIMAL(6,2)  NOT NULL,
    max_distance_km DECIMAL(6,2)  NOT NULL,
    fee             DECIMAL(10,2) NOT NULL,
    sort            INT           NOT NULL DEFAULT 0,
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tier_shop (shop_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='店铺分段运费';

-- 4. 存量数据归入默认店 id=1（幂等：已存在则跳过/不覆盖）
INSERT INTO shop (id, code, name, shop_lat, shop_lng, status, sort)
SELECT 1, 'main', '总店', c.shop_lat, c.shop_lng, 'OPEN', 0
FROM system_config c WHERE c.id = 1
ON DUPLICATE KEY UPDATE shop_lat = VALUES(shop_lat), shop_lng = VALUES(shop_lng);

INSERT INTO shop_config (shop_id, shop_lat, shop_lng, delivery_radius_km, delivery_min_amount,
                         delivery_fee_type, order_time_enabled, order_start_time, order_end_time,
                         qywx_webhook_url_enc, has_qywx_webhook, payment_qr_url,
                         ad_enabled, ad_image_url, ad_link_type, ad_link_target, updated_by)
SELECT 1, c.shop_lat, c.shop_lng, c.delivery_radius_km, c.delivery_min_amount,
       c.delivery_fee_type, c.order_time_enabled, c.order_start_time, c.order_end_time,
       c.qywx_webhook_url_enc, c.has_qywx_webhook, c.payment_qr_url,
       c.ad_enabled, c.ad_image_url, c.ad_link_type, c.ad_link_target, c.updated_by
FROM system_config c WHERE c.id = 1
ON DUPLICATE KEY UPDATE shop_id = shop_id;

INSERT INTO shop_delivery_tier (shop_id, min_distance_km, max_distance_km, fee, sort)
SELECT 1, t.min_distance_km, t.max_distance_km, t.fee, t.sort
FROM system_config_delivery_tier t WHERE t.config_id = 1;

-- 5. 业务表加 shop_id（存量数据默认归店 1；sku 同时加店铺级上下架状态）
ALTER TABLE sku
    ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 1 AFTER product_id,
    ADD COLUMN status  VARCHAR(16) NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE / OFF_SALE（店铺级）' AFTER stock;

ALTER TABLE orders        ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 1 AFTER order_no;
ALTER TABLE order_item    ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 1 AFTER order_id;
ALTER TABLE appointment   ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 1 AFTER order_id;
ALTER TABLE member        ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE member_level  ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE member_phone  ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 1 AFTER member_id;
ALTER TABLE admin_user    ADD COLUMN shop_id BIGINT NULL COMMENT 'NULL=总部（BOSS）' AFTER role;
ALTER TABLE operation_log ADD COLUMN shop_id BIGINT NULL COMMENT 'NULL=总部操作' AFTER user_id;

-- 6. 索引与唯一约束
ALTER TABLE orders
    ADD INDEX idx_orders_shop_create (shop_id, create_time),
    ADD INDEX idx_orders_shop_processed (shop_id, processed);

ALTER TABLE sku
    ADD INDEX idx_sku_shop_product (shop_id, product_id),
    ADD UNIQUE KEY uk_sku_shop_spec (shop_id, product_id, spec_name);

ALTER TABLE appointment ADD INDEX idx_appointment_shop_time (shop_id, start_time, end_time);
ALTER TABLE member        ADD INDEX idx_member_shop (shop_id);
ALTER TABLE member_level  ADD INDEX idx_member_level_shop (shop_id);

-- 同一手机号在不同店可对应不同会员
ALTER TABLE member_phone
    DROP INDEX uk_member_phone,
    ADD UNIQUE KEY uk_member_phone_shop (shop_id, phone);

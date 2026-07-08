-- 开屏广告字段迁移（已部署环境增量执行；新环境由 init.sql 直接建好）
-- 仿 migration_appointment_v1.sql 风格。幂等：执行前可用 SHOW COLUMNS 确认是否已加。
ALTER TABLE system_config
    ADD COLUMN ad_enabled     TINYINT      NOT NULL DEFAULT 0 COMMENT '开屏广告开关：1=开' AFTER payment_qr_url,
    ADD COLUMN ad_image_url   VARCHAR(512) NULL COMMENT '开屏广告图地址' AFTER ad_enabled,
    ADD COLUMN ad_link_type   VARCHAR(16)  NULL COMMENT '开屏广告跳转类型：NONE/PRODUCT/URL' AFTER ad_image_url,
    ADD COLUMN ad_link_target VARCHAR(255) NULL COMMENT '开屏广告跳转目标（productId 或外链）' AFTER ad_link_type;

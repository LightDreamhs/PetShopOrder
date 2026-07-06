-- ============================================
-- 【历史迁移脚本】服务预约系统 第1期 数据库迁移
-- 状态：init.sql 已包含本脚本的全部结构变更，**全新部署无需执行**。
-- 仅适用：在预约系统上线前已存在的旧 petshop_order 库上，增量补 product.service_category、
--         sku.duration、orders.cancelled、main_service_addon、appointment 等结构。
-- 执行方式（docker 环境）：
--   docker exec -i petorder-mysql mysql -uroot -proot123 petshop_order < backend/sql/migration_appointment_v1.sql
-- ============================================

SET NAMES utf8mb4;

-- ---------- 1. product 表加 service_category ----------
ALTER TABLE product
  ADD COLUMN service_category VARCHAR(16) NULL
  COMMENT '服务子类：MAIN_SERVICE 主服务 / ADDON_SERVICE 附加服务。仅 type=SERVICE 时有效，GOODS 为 NULL'
  AFTER type;
ALTER TABLE product
  ADD INDEX idx_product_service_category (type, service_category, status);

-- 现有 SERVICE 商品默认归为 MAIN_SERVICE（原本就是独立售卖的服务）
UPDATE product SET service_category = 'MAIN_SERVICE' WHERE type = 'SERVICE' AND service_category IS NULL;

-- ---------- 2. sku 表加 duration ----------
ALTER TABLE sku
  ADD COLUMN duration INT NULL COMMENT '服务时长（分钟）。仅 SERVICE 的 SKU 用，GOODS 为 NULL'
  AFTER member_price;

-- 给现有洗护/美容 SKU 初始化时长（按 init.sql 的默认值同步）
UPDATE sku SET duration = 60  WHERE product_id = 9  AND spec_name = '小型犬（<10kg）';
UPDATE sku SET duration = 75  WHERE product_id = 9  AND spec_name = '中型犬（10-25kg）';
UPDATE sku SET duration = 90  WHERE product_id = 9  AND spec_name = '大型犬（>25kg）';
UPDATE sku SET duration = 75  WHERE product_id = 10 AND spec_name = '小型犬';
UPDATE sku SET duration = 90  WHERE product_id = 10 AND spec_name = '中大型犬';
UPDATE sku SET duration = 90  WHERE product_id = 11 AND spec_name = '基础护理';
UPDATE sku SET duration = 110 WHERE product_id = 11 AND spec_name = '全套精修';
UPDATE sku SET duration = 60  WHERE product_id = 12 AND spec_name = '短毛猫';
UPDATE sku SET duration = 75  WHERE product_id = 12 AND spec_name = '长毛猫';

-- ---------- 3. orders 表加 cancelled ----------
ALTER TABLE orders
  ADD COLUMN cancelled TINYINT NOT NULL DEFAULT 0
  COMMENT '0 正常 / 1 已取消（取消预约时联动置 1，商家后台据此识别）'
  AFTER processed;

-- ---------- 4. 营业时间字段语义更新（仅改注释） ----------
ALTER TABLE system_config
  MODIFY COLUMN order_time_enabled TINYINT NOT NULL DEFAULT 0
  COMMENT '预约营业时段开关：1=限制预约开始时间在 order_start/end_time 内；实物商品下单不受此字段约束';
ALTER TABLE system_config
  MODIFY COLUMN order_start_time TIME NULL COMMENT '可预约开始时间下限（如 09:00）';
ALTER TABLE system_config
  MODIFY COLUMN order_end_time TIME NULL COMMENT '可预约开始时间上限（如 21:00，开区间）';

-- ---------- 5. 新增 main_service_addon 表 ----------
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

-- ---------- 6. 新增 appointment 表 ----------
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

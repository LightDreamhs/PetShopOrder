-- ============================================
-- SKU 独立图片迁移 v1
-- ============================================
-- 适用对象：已按旧版 init.sql 初始化的库（sku 无 img_url、order_item 无 sku_img）。
-- 执行方式：docker exec 进 MySQL 容器执行（一次性脚本，勿重复执行）：
--   docker exec -i petorder-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" petshop_order' < migration_sku_img_v1.sql
-- 新库不需要本脚本：更新后的 init.sql 已原生带列。
-- 兼容性：新增可空列，旧代码可正常运行（向后兼容，回滚代码无需回库）。

SET NAMES utf8mb4;

ALTER TABLE sku
    ADD COLUMN img_url VARCHAR(255) NULL COMMENT 'SKU图片URL' AFTER member_price;

ALTER TABLE order_item
    ADD COLUMN sku_img VARCHAR(255) NULL COMMENT '下单时SKU图片快照' AFTER sku_name;

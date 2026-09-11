# 多店改造 + SKU 图 + Redis 生产上线清单

> 📋 目标版本：master `d5e2c9d` 之后的最新提交（含多店 Phase 1~3、管理端账号体系安全修复、SKU 独立图片、Redis 会话持久化、H5 顾客锁定门店）
> 🖥️ 当前生产版本：`b99d15c`（2026-07-16），单店架构
> ✅ 本文是本次上线的**唯一权威执行清单**，在服务器上从上往下照敲即可。AI 可通过 SSH 协助执行，标记 🔴 的步骤执行前需用户确认。

---

## 0. 上线前决策项（已拍板 2026-09-06）

| 项 | 决策 |
|---|---|
| 短信 | ~~暂不切换~~ **已于 2026-09-12 切换 aliyun**（`SMS_PROVIDER=aliyun`，切换后需真机验证登录；固定码 `123456` 通道关闭） |
| 二江寺店 | **随本次开张，直接营业态创建**（无歇业设计）；顾客入口靠「不发放该店二维码」控制，无参数/旧码落佳兆业店 |
| 会员等级 | 按佳兆业店现有档位结构在二江寺店做一次性快照新增（不搬会员名单，之后两边独立管理） |
| 二江寺店坐标/配送/时段/收款码 | 坐标先填大致值可后调；收款码复制佳兆业；配送与时段默认值上线，用户之后在 Admin 精调 |

## 1. 前置检查

```bash
ssh ubuntu@106.53.178.130
df -h /                          # 磁盘余量（需 >5G）
free -h                          # 内存（available 应 >800Mi）
docker ps                        # 三容器运行中
```

## 2. 全量备份（🔴 必须在迁移前完成）

```bash
mkdir -p /backup/pre-multistore
docker exec petorder-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction petshop_order' > /backup/pre-multistore/petshop-$(date +%F-%H%M).sql
tar -czf /backup/pre-multistore/uploads-$(date +%F-%H%M).tar.gz /data/petshop/uploads
ls -la /backup/pre-multistore/   # 确认两个文件非空
```

## 3. 拉取代码

```bash
cd ~/PetShopOrder
git pull                          # 应看到 master 更新至 d5e2c9d 之后
git log --oneline -5              # 核对包含 7eedd73(SKU图)/0375d76(样式)/c0e4926(Redis) 等提交
```

## 4. 数据库迁移（🔴 顺序执行，每步验证）

> 两个脚本均向后兼容旧代码（新列可空/有默认值，旧表保留），异常时只需回滚代码不需回库。

```bash
# 4.1 多店底座迁移（建 shop/shop_config/shop_delivery_tier、业务表加 shop_id、
#     存量数据归入佳兆业店 id=1、system_config 数据拷贝到 shop_config）
docker exec -i petorder-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" petshop_order' < ~/PetShopOrder/backend/sql/migration_multi_store_v1.sql

# 4.2 SKU 独立图片迁移（sku 加 img_url、order_item 加 sku_img）
docker exec -i petorder-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" petshop_order' < ~/PetShopOrder/backend/sql/migration_sku_img_v1.sql

# 4.3 迁移验证（预期：shop 表有佳兆业店；shop_config 有店1配置；sku.img_url 列存在）
docker exec petorder-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" petshop_order -e "
SELECT id, code, name, status FROM shop;
SELECT shop_id, delivery_radius_km, delivery_fee_type, has_qywx_webhook FROM shop_config;
SHOW COLUMNS FROM sku LIKE \"img_url\";
SHOW COLUMNS FROM order_item LIKE \"sku_img\";
SELECT COUNT(*) AS orders_migrated FROM orders WHERE shop_id = 1;
"'
```

## 5. 环境变量更新

```bash
cd ~/PetShopOrder/deploy
vi .env.prod
```

| 变量 | 操作 |
|---|---|
| `SMS_PROVIDER` | ~~若拍板切换 aliyun~~ **已切换 `aliyun`（2026-09-12）**，AK/SK 已就绪；切换后第 8 步必须真机验证登录 |
| Redis 变量 | 无需添加（compose 已内置 `REDIS_HOST: redis`，容器内网直连） |

## 6. 构建并启动（四容器：mysql + redis + backend + frontend）

```bash
cd ~/PetShopOrder/deploy
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker ps                          # 预期四个容器 Up，petorder-redis 为新增
docker logs petorder-backend --tail 20   # 无 ERROR，出现 Started PetShopOrderApplication
```

> ⚠️ 本次 backend 重建后：**管理员必须重新登录**（cookie 体系改为 satoken-admin，安全修复的预期行为）；C 端顾客会话因 Redis 持久化**不受影响**（若顾客在旧版本登录过，其旧内存会话已随旧进程消失，需重新登录一次）。

## 7. 冒烟验证（浏览器 + curl）

- [ ] `https://2zg.site/health` 返回 UP
- [ ] H5 首页正常、商品列表正常（旧裸地址码扫入落佳兆业店）
- [ ] H5 登录：**真机收验证码**（若已切 aliyun；log 模式则固定码 123456）
- [ ] Admin 登录 → 订单/会员/系统配置页数据正常（均为佳兆业店）
- [ ] Admin「门店管理」可见佳兆业店，「系统配置」可编辑保存
- [ ] H5 下单一单 → Admin 订单页可见
- [ ] Redis 会话：`docker exec petorder-redis redis-cli --scan --pattern 'satoken*' | head`
- [ ] 🔴（可选但推荐）`docker restart petorder-backend` → 刷新 H5 **不掉线**

## 8. 二江寺店开张（营业态，入口靠不发放二维码控制）

方式一（推荐，Admin 界面操作）：**门店管理 → 新增门店**，填编码 `erjiangsi`、名称 `二江寺店`、坐标（先填二江寺地铁站大致位置，如 `30.5482, 104.0000` 附近，后续地图选点精调）→ 创建（默认营业）。

方式二（SQL）：

```bash
docker exec petorder-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" petshop_order --default-character-set=utf8mb4 -e "
INSERT INTO shop (code, name, status, sort) VALUES (\"erjiangsi\", \"二江寺店\", \"CLOSED\", 2);
SET @s2 = (SELECT id FROM shop WHERE code = \"erjiangsi\");
INSERT INTO shop_config (shop_id, shop_lat, shop_lng, delivery_radius_km, delivery_min_amount, delivery_fee_type) VALUES (@s2, 30.5482000, 104.0000000, 3.00, 20.00, \"FREE\");
\"'
```

**等纷新增（纯新增，不动佳兆业数据）+ 收款码复制**：

```bash
docker exec petorder-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" petshop_order --default-character-set=utf8mb4 -e "
SET @s2 = (SELECT id FROM shop WHERE code = \"erjiangsi\");
INSERT INTO member_level (shop_id, name, discount_rate, sort, status)
SELECT @s2, name, discount_rate, sort, status FROM member_level WHERE shop_id = 1;
UPDATE shop_config SET payment_qr_url = (SELECT payment_qr_url FROM (SELECT payment_qr_url FROM shop_config WHERE shop_id = 1) t) WHERE shop_id = @s2;
SELECT id, shop_id, name, discount_rate FROM member_level WHERE shop_id = @s2;
"'
```

## 9. 用户手动配置（Admin 界面）

- [ ] 二江寺店：地图选点精调坐标（编辑门店 → 保存，自动同步配送计算）、配送半径/起送价/运费分段（系统配置页切到二江寺店）、营业时段、按需修改门店地址/电话（当前 H5 无展示位，仅存档）
- [ ] （可选）创建店长账号：账号管理 → 新增 → 归属店选二江寺店

## 10. 回滚方案（异常时）

```bash
# 数据库不回滚（迁移向后兼容旧代码），仅回滚代码：
cd ~/PetShopOrder && git checkout b99d15c
cd deploy && docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build backend frontend
# redis 容器可保留（旧代码不读它）
```

## 11. 上线后收尾

- [x] `free -h` 复查内存水位（Redis 稳态 <50MB）
- [x] 观察 3~7 天后下线旧表：`DROP TABLE system_config_delivery_tier, system_config, system_config_log;`（✅ 已于 2026-09-12 执行，DROP 前已全量备份）
- [ ] 生产 `git log` 核对与远端 master 一致

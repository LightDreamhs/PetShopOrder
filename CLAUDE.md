# PetShopOrder 项目规范

## 服务器信息

| 项目 | 信息 |
|------|------|
| **IP** | 106.53.178.130 (腾讯云 Ubuntu) |
| **SSH** | `ssh ubuntu@106.53.178.130` |
| **Docker** | 27.5.1 / Compose v2.32.4 |

---

## 包管理器

- 前端使用 **pnpm**，不是 npm

## 数据库表结构

| 表 | 全部字段 | 业务意义 | H5 作用 | Admin 作用 |
|---|---|---|---|---|
| admin_user | id, username, password_hash, real_name, role, status, last_login_time, create_time, update_time | 管理端账号与角色（BOSS/MANAGER/STAFF） | 无直接使用 | LoginPage 登录鉴权；AdminUserPage 账号增删改、启停用、重置密码；路由菜单权限控制 |
| app_user | id, phone, status, last_login_time, create_time, update_time | C 端手机号登录用户 | LoginPage 验证码登录；全站登录态（首页/购物车/结算/订单） | 无直接页面 |
| product | id, name, description, cover_img, type, status, support_delivery, sort, create_time, update_time | 商品/服务主表 | HomePage 列表展示；CheckoutPage 下单计算时按类型/配送属性参与规则 | ProductPage 商品列表、上下架、编辑、新增、删除 |
| sku | id, product_id, spec_name, price, member_price, stock, sort, create_time, update_time | 规格与价格明细（库存也在这里） | HomePage 详情/规格弹窗；CartPage/CheckoutPage 按 SKU 算价；下单时按 SKU 落单 | ProductPage 规格管理（规格名、原价、会员价、库存、排序） |
| member_level | id, name, discount_rate, sort, status, create_time, update_time | 会员等级与服务折扣率 | HomePage/CartPage/CheckoutPage 会员价/折扣展示和算价依据（通过后端） | MemberPage 等级管理（增删改、启停用） |
| member | id, name, level_id, remark, create_time, update_time | 会员档案（等级归属） | HomePage/CartPage/CheckoutPage 会员身份命中后影响价格与提示 | MemberPage 会员列表与编辑 |
| member_phone | id, member_id, phone, create_time, update_time | 会员与手机号映射（一个会员多手机号） | 登录手机号命中会员（决定会员价/折扣） | MemberPage 编辑会员手机号时维护映射 |
| orders | id, order_no, user_id, customer_phone_snapshot, customer_name, member_id, member_level_snapshot, goods_amount, service_amount, delivery_fee, total_amount, need_delivery, delivery_address, delivery_lat, delivery_lng, delivery_distance, processed, remark, create_time, update_time | 订单主表（金额、配送、处理状态、快照） | CheckoutPage 创建订单；OrderListPage/OrderDetailPage 查询订单；OrderSuccessPage 展示下单结果 | OrderPage 列表筛选、详情查看、标记已处理/未处理 |
| order_item | id, order_id, product_id, sku_id, type, product_name, sku_name, original_price, deal_price, quantity, subtotal, create_time, update_time | 订单明细快照（防止商品后续改名/改价影响历史订单） | OrderDetailPage 明细展示 | OrderPage 详情抽屉明细与汇总 |
| system_config | id, shop_lat, shop_lng, delivery_radius_km, delivery_min_amount, delivery_fee_type, fixed_delivery_fee, order_time_enabled, order_start_time, order_end_time, qywx_webhook_url_enc, has_qywx_webhook, updated_by, create_time, update_time | 全局系统配置（配送规则、接单时段、通知） | CheckoutPage/cart calculate、下单流程：配送可达、起送价、运费规则（经后端） | SystemConfigPage 配置读写、Webhook 测试 |
| system_config_delivery_tier | id, config_id, min_distance_km, max_distance_km, fee, sort, create_time, update_time | 分段运费规则明细 | H5 结算/下单时参与运费计算（后端） | SystemConfigPage 分段规则编辑 |
| system_config_log | id, config_id, operator_id, operator_name, summary, before_val, after_val, create_time, update_time | 系统配置变更审计日志 | 无直接页面 | SystemConfigPage 下方"配置变更记录" |
| operation_log | id, user_id, action, target, before_val, after_val, create_time | 通用后台操作日志审计 | 无 | 后端有 /api/admin/logs，admin 前端暂未挂日志页面路由 |

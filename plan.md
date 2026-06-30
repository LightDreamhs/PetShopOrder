LD，需求已经清晰，现在给出完整方案。

> ⚠️ **历史档案（2026-06-30 标注）**
>
> 本文件是项目最初的技术方案文档，**部分决策在后续开发中已被推翻**，仅供追溯设计意图，**不反映当前实现**。下方列出的内容已与现状不符，请以 [`modules.md`](modules.md) / [`api.md`](api.md) / 代码实现为权威：
>
> - **商品分类子分类**：方案设计了 `category` 表 + 分类 CRUD，实际已下线，仅保留固定 GOODS/SERVICE 两类。
> - **配送运费**：方案设计为「≤3km 免费 / >3km 不自动计费，标记 `delivery_status=OVER_RANGE` 电话确认」，实际改为按 `system_config` 运费策略（FREE / TIERED 分段）统一计算，无 OVER_RANGE 态。
> - **FIXED 固定运费策略**：方案设计了 FREE/FIXED/TIERED 三策略，实际仅实现 FREE/TIERED。
> - **新订单通知兜底**：方案设计用 WebSocket + 浏览器 Notification API，实际改用 15 秒轮询 `GET /api/admin/orders/new-count` + ElNotification。
> - **企微机器人**：方案仅提企业微信，实际已扩展支持飞书 Webhook。
> - **短信验证码**：方案设计为自研验证码逻辑，实际改造方向见 [`deploy/sms-pnvs-planning/实施计划.md`](deploy/sms-pnvs-planning/实施计划.md)（阿里云号码认证服务 PNVS）。

---

# 宠物店微信私域 H5 下单系统 - 技术方案

## 一、需求总览

| 维度     | 决策                                                              |
| -------- | ----------------------------------------------------------------- |
| 用户端   | 微信扫码进入 H5 后先手机号+验证码登录；登录态自动续期，后续扫码默认免重复登录；下单手机号取账户信息 |
| 用户功能 | 商品分类浏览（图片+SKU）→ 购物车 → 下单（备注，不真实支付）       |
| 会员体系 | 登录后按手机号匹配会员库；多等级，等级决定服务折扣率与用品定价 |
| 配送     | 腾讯地图选点；订单原价≥¥20 才能配送；3公里内自动免费/计费，超 3km 商家电话确认 |
| 商家端   | PC Web 后台（响应式，手机也能用），多角色权限                     |
| 通知方案 | **企业微信群机器人** Webhook 推送（零成本、零认证）               |
| 服务预约 | 下单不选时间，备注联系；服务只能到店，不参与配送                  |
| 订单     | 仅记录，不做状态流转                                              |
| 业务规模 | 单店自用                                                          |

---

## 二、技术栈

### 后端（Java 生态）
- **Spring Boot 3.x** + JDK 17（LTS）
- **MyBatis**
- **MySQL 8.0**
- **Sa-Token**（轻量权限框架，比 Spring Security 配置简单 10 倍，完美匹配单店多角色场景）
- 图片存储在云服务器本地
- **HuTool**（工具库）

### 前端（两套，同一技术栈）
- **Vue 3 + Vite + TypeScript** + **Pinia**
- **用户端 H5**：**Vant 4**（移动端事实标准，购物车/SKU 选择器现成组件）
- **商家管理端**：**Element Plus**（PC 后台首选，表格/表单完善，响应式可适配手机）
- **地图组件**：**腾讯位置服务 JavaScript API GL** + **WebServiceAPI**（用户端嵌入选点组件）

### 部署
- **Docker Compose** 单机部署（单店自用无需 K8s）
- **Nginx** 反向代理 + 静态文件托管
- **Caddy** 可选（自动 HTTPS，比 Nginx 配置简单）

---

## 三、核心架构

```
┌─────────────────────────────────────────────────────────┐
│  微信内 H5（Vue3+Vant+腾讯地图）  管理端（Vue3+Element Plus）│
│        ↓ 扫码二维码                    ↓ 浏览器访问      │
└─────────────────────────────────────────────────────────┘
                       ↓ HTTPS
              ┌────────────────────┐
              │   Nginx / Caddy    │  ← 静态资源 + 反代
              └────────────────────┘
                       ↓
              ┌────────────────────────────┐
              │ Spring Boot 3 API          │
              │  - 商品/SKU/分类           │
              │  - 手机号验证码登录/续期    │
              │  - 会员服务（等级/匹配）   │
              │  - 价格计算引擎            │
              │  - 配送费计算（含腾讯API） │
              │  - 订单服务                │
              │  - 文件上传                │
              │  - 企微机器人推送          │ ─────→ 企业微信群
              │  - Sa-Token 鉴权           │
              └────────────────────────────┘
                       ↓                      ↓
              ┌──────────┐  ┌──────────┐  ┌──────────────┐
              │ MySQL 8  │  │ 本地存储 │  │ 腾讯位置服务 │
              └──────────┘  └──────────┘  └──────────────┘
```

---

## 四、会员体系设计

### 4.1 识别方式
用户在扫码进入 H5 后先完成**手机号+验证码登录**（登录态自动续期）。  
后端按登录账户手机号唯一匹配 `member` 表。  
匹配命中即此账号按对应等级享受会员权益；未命中按非会员计价。

### 4.2 价格规则
| 类型     | 非会员 | 会员                                              |
| -------- | ------ | ------------------------------------------------- |
| 用品     | 原价   | SKU 上的"会员价"字段（统一会员价，不分等级）      |
| 服务     | 原价   | 原价 × 会员等级折扣率（如黄金 0.85、铂金 0.80）   |

> 用品采取「原价 / 会员价」两档，会员等级仅决定能否享会员价；服务按等级折扣。  
> 价格计算在**后端**完成（前端可预览，后端权威下单）。

### 4.3 用户端展示
商品/服务详情、购物车、结算页**明表展示**：
```
原价 ¥168   会员价 ¥135（黄金会员 8.5折）
```
未识别为会员时，仅显示原价；结算页提示「您不是会员，已按原价结算」。

---

## 五、配送方案设计

### 5.1 触发与门槛
- 购物车中**至少一个用品**才出现"配送"开关；纯服务订单不显示
- 勾选"配送"前会校验：**购物车原价**（不含会员折扣）的**用品部分** ≥ ¥20，否则提示「还差 ¥X 起送」并禁用勾选
- 服务部分始终到店，不计入配送

### 5.2 距离与运费规则
| 距离      | 处理                                                |
| --------- | --------------------------------------------------- |
| ≤ 3km    | 免运费                                              |
| > 3km     | **不自动计费**，订单标记"超距待商家确认"，企微通知会高亮提示，商家电话沟通运费补付 |

### 5.3 技术实现（腾讯地图）
**为什么选腾讯**：
- 个人开发者可在 [lbs.qq.com](https://lbs.qq.com) 免费申请 Key
- JavaScript API GL：**月配额免费**，无明显限制
- WebService API（地址逆解析、距离计算）：**1万次/日免费**
- 单店一天 20 单 = 一天约 40-60 次调用，**1/200 的免费额度**，绰绰有余
- 高德同样够用，腾讯申请门槛更低（不强制实名）

**用户端流程**：
1. 勾选"配送"→ 弹出腾讯地图选点组件
2. 默认调用 H5 定位接口定位用户当前位置
3. 用户拖动大头针或搜索关键词选点
4. 选点后调"逆地址解析"自动填入详细地址
5. 提交订单时把 `lat/lng/address` 一起带上

**后端流程**：
1. 配置店铺坐标（`shop.lat / shop.lng`，后台可改）
2. 收到订单时算店铺与用户坐标的**球面直线距离**（Haversine 公式，本地算，零调用）
3. ≤3km：运费 0；>3km：标记 `delivery_status = OVER_RANGE`，企微通知「⚠️ 配送距离 4.2km，需电话确认运费」

> 直线距离够用：宠物店配送是骑手跑，3km 直线 ≈ 3.5-4km 路程；想精确可调腾讯"距离矩阵 API"，但不必要。

---

## 六、数据库设计

```sql
-- ============ 商品体系 ============

-- 商品分类
category(id, name, sort, icon, type)
  -- type: GOODS / SERVICE

-- 商品/服务（统一表，type 区分）
product(id, category_id, name, description, cover_img,
        type, status, support_delivery, sort, create_time)
  -- type: GOODS / SERVICE
  -- status: ON_SALE / OFF_SALE
  -- support_delivery: 仅 GOODS 有效，SERVICE 强制为 0

-- SKU（多规格）
sku(id, product_id, spec_name, price, member_price, stock, sort)
  -- price: 原价
  -- member_price: 会员价（仅 GOODS 用，SERVICE 此字段忽略）
  -- spec_name: "5kg" / "金毛-中型犬-基础洗"
  -- stock: SERVICE 可为 -1 或忽略

-- ============ 会员体系 ============

-- C 端用户账号（手机号验证码登录）
app_user(id, phone, status, last_login_time, create_time)
  -- phone: 登录手机号（建唯一索引）
  -- status: 0/1（禁用后不可下单）

-- 会员等级
member_level(id, name, discount_rate, sort, status)
  -- name: "黄金会员" / "铂金会员" / "钻石会员"
  -- discount_rate: 服务折扣率，0.85 表示 8.5 折
  -- sort: 排序
  -- status: 0/1

-- 会员
member(id, name, phone, level_id, remark, create_time)
  -- phone: 与 app_user.phone 对应（建唯一索引）
  -- level_id -> member_level.id
  -- 仅基础信息，不做余额/计次卡

-- ============ 订单 ============

orders(id, order_no, user_id, customer_phone_snapshot, customer_name,
       member_id, member_level_snapshot,
       goods_amount, service_amount, delivery_fee, total_amount,
       need_delivery, delivery_status,
       delivery_address, delivery_lat, delivery_lng, delivery_distance,
       remark, create_time)
  -- user_id -> app_user.id（必填，标识下单账户）
  -- customer_phone_snapshot: 下单时手机号快照（防止后续改绑影响历史订单）
  -- customer_name: 联系人名称，可选
  -- member_id: 命中会员则记录，否则 NULL
  -- member_level_snapshot: 下单时会员等级名快照（防止后续等级被改影响历史订单）
  -- goods_amount: 用品金额合计（已折扣）
  -- service_amount: 服务金额合计（已折扣）
  -- delivery_fee: 系统算出的运费（≤3km=0；>3km=NULL，待商家确认）
  -- total_amount: goods_amount + service_amount + (delivery_fee or 0)
  -- need_delivery: 0/1
  -- delivery_status: NONE(无需配送) / IN_RANGE(3km内) / OVER_RANGE(超距待确认)
  -- delivery_distance: 与店铺直线距离，单位米

-- 订单明细
order_item(id, order_id, product_id, sku_id, type,
           product_name, sku_name,
           original_price, deal_price, quantity, subtotal)
  -- type: GOODS / SERVICE（冗余，便于报表）
  -- original_price: 下单时原价快照
  -- deal_price: 下单时实付单价快照（已应用会员价或服务折扣）
  -- subtotal: deal_price * quantity

-- ============ 系统 ============

-- 后台账号
admin_user(id, username, password, real_name, role, status)
  -- role: BOSS / MANAGER / STAFF

-- 操作日志
operation_log(id, user_id, action, target, before, after, time)

-- 系统配置（K-V 表，店铺坐标、配送规则、企微机器人地址等）
sys_config(id, config_key, config_value, description, update_time)
  -- 例：shop.lat / shop.lng / delivery.min_amount(20)
  --     delivery.free_distance_km(3) / wechat.bot_url

-- ============ 索引 ============
orders.order_no UNIQUE
INDEX idx_orders_create_time(create_time)
INDEX idx_orders_user_id(user_id)
INDEX idx_orders_member_id(member_id)
INDEX idx_order_item_order_id(order_id)
INDEX idx_sku_product_id(product_id)
app_user.phone UNIQUE
member.phone UNIQUE
```

**多角色权限矩阵**：
| 功能         | 老板 | 店长 | 店员 |
| ------------ | ---- | ---- | ---- |
| 查看订单     | ✓    | ✓    | ✓    |
| 商品/服务管理 | ✓    | ✓    | ✗    |
| 改价         | ✓    | ✓    | ✗    |
| 会员管理     | ✓    | ✓    | ✗    |
| 会员等级管理 | ✓    | ✗    | ✗    |
| 系统配置     | ✓    | ✗    | ✗    |
| 账号管理     | ✓    | ✗    | ✗    |
| 数据统计     | ✓    | ✓    | ✗    |

---

## 七、新订单通知方案

```
🐾 新订单 #20260423001  ⭐黄金会员
━━━━━━━━━━━━━━━━━━━━━━━━━━━
📱 会员手机号：138****8888
👤 联系人：张先生
━━━━━━━━━━━━━━━━━━━━━━━━━━━
🛍 用品（已享会员价）
  · 金毛粮5kg  ¥135 ×1   = ¥135
🧴 服务（黄金 8.5 折）
  · 中型犬洗澡  ¥85 ×1    = ¥85
━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚚 配送：是  距离 2.3km  免运费
📍 朝阳区XX路XX号XX小区3号楼
━━━━━━━━━━━━━━━━━━━━━━━━━━━
💰 总计：¥220（原价 ¥260，省 ¥40）
📝 备注：周六下午来取，狗狗有点皮肤病
⏰ 2026-04-23 14:32
```

**超距订单**额外加一段：
```
⚠️ 配送距离 4.2km，超出免费范围，请电话沟通运费
```

**容灾**：管理后台 WebSocket 推送 + 浏览器 Notification API 弹窗，老板电脑开着后台就能收到。

---

## 八、价格计算引擎（后端核心）

下单是后端独立做一遍价格计算，不信任前端：

```
1. 解析购物车 items（product_id + sku_id + qty）
2. 从登录态获取 `user_id`，查询账户手机号并匹配会员，得到 member_level 与 discount_rate
3. 对每个 item：
   - GOODS: deal_price = 命中会员 ? sku.member_price : sku.price
   - SERVICE: deal_price = 命中会员 ? sku.price * discount_rate : sku.price
   - subtotal = deal_price * qty
4. goods_amount = sum(GOODS 类 subtotal)
   service_amount = sum(SERVICE 类 subtotal)
5. 配送校验：
   - need_delivery=true 时：
     - 校验购物车 GOODS 部分的"原价合计"≥ 20，否则报错
     - 计算距离 distance = haversine(shop, user)
     - distance ≤ 3000m → delivery_fee=0, status=IN_RANGE
     - 否则 → delivery_fee=null, status=OVER_RANGE
6. total = goods_amount + service_amount + (delivery_fee or 0)
7. 写入 orders + order_item（带手机号快照、会员等级快照）
8. 异步推企微
```

---

## 九、字段规则与校验约定

- 下单前需完成手机号验证码登录；下单时不再手填手机号
- 订单手机号来自 `customer_phone_snapshot`（登录账户手机号自动带入，不可手填）
- 下单表单：`customer_name` 可选；`remark` 可选
- 会员匹配：按登录账户手机号匹配会员，命中即享对应等级权益
- 配送地址：选了配送才必填 `address + lat + lng`
- 企微通知：识别会员时附带等级图标，超距订单高亮警告

---

## 十、域名备案流程（阿里云为例）

| 阶段        | 操作                           | 时长                      |
| ----------- | ------------------------------ | ------------------------- |
| 1. 准备     | 身份证、个人手持照片、域名证书 | 1天                       |
| 2. 提交     | 阿里云 → 备案系统 → 个人备案   | 30分钟                    |
| 3. 阿里初审 | 阿里云审核材料                 | 1-2 工作日                |
| 4. 短信核验 | 工信部短信发到你手机，回复确认 | 当天                      |
| 5. 管局审核 | 各省通信管理局审核             | **3-20 工作日**（看省份） |
| 6. 备案下发 | 拿到备案号，可解析使用         | -                         |

**关键提醒**：
- 必须先买阿里云服务器（≥3个月）才能用阿里云的备案通道
- 备案期间域名不能使用，建议**先备案后开发**或**开发用 IP+测试域名**
- 个人备案不能做经营性内容，宠物店最好用「个人」+「博客/展示」名义提交
- 整体预留 1 个月时间

---

## 十一、关键交互流程

**用户下单流程**：
```
扫描店内二维码 → H5 首页（Banner+分类）
  → 首次进入手机号+验证码登录（登录态有效期内再次扫码免重复登录）
  → 选分类 → 商品列表 → 点击商品 → 选 SKU → 加购物车
  → 购物车结算
       ├─ 填姓名（可选）+备注
       ├─ 是否配送（仅含用品时显示）
       │    └─ 勾选 → 校验用品原价 ≥20 → 弹腾讯地图选点
       └─ 实时显示价格预览（含会员价/折扣）
  → 提交订单（后端权威算价）
  → 订单成功页（订单号+总价+超距提示）
       ↓
  后端异步推企微 → 老板群里收到通知
```

**配送规则示例**：
| 购物车           | 用品原价 | 勾选配送 | 距离  | 结果                 |
| ---------------- | -------- | -------- | ----- | -------------------- |
| 仅服务           | -        | 不显示   | -     | 强制到店             |
| 用品 ¥15         | ¥15      | 被禁     | -     | 提示「还差¥5起送」   |
| 用品 ¥30         | ¥30      | 可选     | 2.3km | 免费配送             |
| 用品 ¥30+服务¥80 | ¥30      | 可选     | 4.5km | 标记超距，电话确认   |

**商家改价流程**：
```
登录后台 → 商品管理 → 选商品 → 改 SKU 原价/会员价 → 保存
  → 写入 operation_log
  → 用户端立即生效
```

---

## 十二、开发计划

| 阶段                   | 内容                                                           | 预估              |
| ---------------------- | -------------------------------------------------------------- | ----------------- |
| **P0 准备**            | 买服务器、买域名、提交备案、申请腾讯地图Key                    | 第1周（备案并行） |
| **P1 后端基础**        | 项目初始化、数据库、商品/SKU/分类、Sa-Token、文件上传          | 1周               |
| **P2 后端业务**        | 会员模块、价格引擎、距离计算、订单、企微推送                   | 4天               |
| **P3 管理端**          | 登录、商品/服务CRUD、会员CRUD、会员等级、订单列表、系统配置    | 1周               |
| **P4 用户端 H5**       | 首页、列表、详情、购物车、地图选点、结算下单                   | 1周               |
| **P5 联调**            | 全链路联调、超距通知、WebSocket 兜底                           | 3天               |
| **P6 部署上线**        | Docker 部署、HTTPS、二维码生成、店铺坐标配置                   | 2天               |

总计约 5-6 周（备案在 P0 与 P1-P5 并行不阻塞开发）。

---

## 十三、可能的坑（提前规避）

1. **微信内 H5 缓存**：发布新版本时 HTML 加版本号 query，JS/CSS 用 Vite 默认 hash
2. **iOS 软键盘遮挡**：购物车结算页注意 viewport 适配
3. **图片上传**：放在云服务器本地，需做磁盘空间监控；后期可平滑迁 OSS（接口抽象）
4. **企微机器人限频**：每个机器人每分钟最多 20 条，单店完全够用
5. **订单号生成**：用「日期+雪花算法」，避免单纯自增暴露业务量
6. **腾讯地图 Key 防盗刷**：在腾讯后台配置「Referer 白名单」+「日配额上限」，防被恶意调用
7. **手机号唯一与改号流程**：`member.phone` 与 `app_user.phone` 均设唯一索引；改绑手机号需验证码复核；历史订单保留 `customer_phone_snapshot` 快照
8. **价格快照**：订单明细必须存 `original_price` 和 `deal_price` 快照，否则商家改价/会员等级调整后历史订单金额会"漂移"
9. **超距订单状态**：商家电话确认运费后，需要在管理后台「补填运费」按钮，更新 `delivery_fee` 与 `total_amount`
10. **服务+用品混装**：明确告知用户「服务到店、用品配送」，避免误解

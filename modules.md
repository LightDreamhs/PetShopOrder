# 宠物店微信私域 H5 下单系统 - 功能模块清单

> 基于 plan.md 整理，参考 MVP 原型补充交互细节。面向开发自用，按后端/前端拆分。
>
> **修订（2026-06-30）**：已与代码实现核对并标注现状差异：① 商品「分类子分类」功能已下线，仅保留固定 GOODS/SERVICE 两类（无分类 CRUD）；② 新订单通知改用 `GET /api/admin/orders/new-count` 轮询替代 WebSocket；③ 运费策略仅实现 FREE/TIERED，FIXED 未实现；④ 配送费改为按 `system_config` 运费策略计算，不再是「≤3km 免费/超距电话确认」。下方涉及处以「现状」标注。权威实现以代码为准。
>
> **修订（2026-07-08）**：补充服务预约系统（已上线）。服务不再像商品一样直接下单，改为走独立预约入口——主服务 + 附加服务 + 选开始时间，并带全局容量冲突检测（任意时刻同时进行的服务 ≤ 3 个）。涉及 `product.service_category`、`sku.duration`、`orders.cancelled` 字段及 `appointment`、`main_service_addon` 两张新表，详见「12. 服务预约系统模块」与「三、数据库设计 → 预约体系」。

---

---

## 一、后端模块（Spring Boot 3 + MyBatis + MySQL 8）

> **数据库通用规约**：所有表均需包含 `id`（BIGINT 自增主键）、`create_time`、`update_time` 三个必备字段。更新操作必须同步更新 `update_time`。

### 1. 认证与登录模块

**功能点**：
- C 端手机号验证码登录：`/api/app/auth/sms-code`、`/api/app/auth/login`
- C 端登录态检查与退出：`/api/app/auth/check`、`/api/app/auth/logout`
- 后台账号登录：`/api/admin/auth/login`
- 后台登录态检查与退出：`/api/admin/auth/profile`、`/api/admin/auth/logout`

**业务规则**：
- C 端：手机号唯一，首次登录自动创建 `app_user` 记录
- 后台：账号角色分为 BOSS / MANAGER / STAFF，密码哈希存储
- 登录态建议使用 HttpOnly Cookie（前端仅通过 profile/check 感知登录态）

**边界条件**：
- 同一手机号短时间内验证码发送限频
- 后台账号连续错误登录需锁定或验证码

---

### 2. 商品模块

**功能点**：
- ~~商品分类 CRUD（type 区分 GOODS / SERVICE）~~ → **现状**：分类子分类已下线，仅保留固定 `GOODS`/`SERVICE` 两类，无分类 CRUD
- 商品 CRUD（含封图、描述、上下架、排序）
- SKU CRUD（多规格，含原价、会员价、库存）
- 图片上传（存储在服务器本地）

**业务规则**：
- ~~商品 type 不可跨分类混用（分类的 type 决定商品的 type）~~ → **现状**：商品直接归属 GOODS/SERVICE 类型
- SERVICE 类型商品用 `service_category` 区分两类：`MAIN_SERVICE`（主服务，可预约，走预约入口）与 `ADDON_SERVICE`（附加服务，随主服务多选，不单独售卖）；GOODS 该字段为 NULL
- SERVICE 类型商品 `support_delivery` 强制为否，SKU 的 `member_price` 字段忽略
- SERVICE 的 SKU 配 `duration`（分钟，店主自填），用于预约总时长计算；GOODS 该字段为 NULL
- GOODS 类型 SKU 的 `stock` 必填，SERVICE 可为 -1（不限）
- 商品下架后用户端不可见，但历史订单不受影响

**边界条件**：
- ~~删除分类前需检查是否有关联商品~~ → **现状**：无分类概念，已删除商品需检查是否被订单引用
- SKU 价格修改需记录操作日志（用于追溯改价历史）

---

### 3. 会员模块

**功能点**：
- 会员等级 CRUD（等级名、服务折扣率、排序、启停用）
- 会员信息 CRUD（姓名、多个手机号、等级、备注）
- C 端用户自动匹配会员：按登录手机号匹配 `member_phone` 映射表

**业务规则**：
- 匹配命中 → 按等级享受会员权益；未命中 → 按非会员计价
- 用品：会员统一使用 SKU 的 `member_price`，不分等级
- 服务：按会员等级的 `discount_rate` 折扣（如 500 档 0.95 = 9.5 折、2000 档 0.85 = 8.5 折）
- 会员手机号全局唯一（同一手机号只能挂一个会员）

**边界条件**：
- 一个手机号只能绑定一个会员
- 不允许停用有会员关联的等级，必须先将该等级下所有会员调整到其他等级后才能停用
- 改绑手机号需验证码复核

---

### 4. 价格计算引擎

**功能点**：
- 下单时后端权威算价（不信任前端传入价格）
- 会员价/折扣实时计算
- 购物车金额预览接口（前端调用展示）

**业务规则**：
- GOODS: `deal_price = 命中会员 ? (sku.member_price ?? sku.price) : sku.price`（member_price 为 NULL 时回退到原价）
- SERVICE: `deal_price = 命中会员 ? sku.price * discount_rate : sku.price`（折扣后立即 HALF_UP 到分）
- `subtotal = deal_price * quantity`
- `goods_amount = sum(GOODS 类 subtotal)`
- `service_amount = sum(SERVICE 类 subtotal)`
- C 端商品列表 price = min(sku.price)，dealPrice = min(sku 的成交价)

**金额精度规则**：
- 数据库金额字段统一 `DECIMAL(10,2)`，折扣率字段用 `DECIMAL(5,4)`
- **舍入时机**：单价计算完成后立即四舍五入到分，再乘以数量算小计
- **舍入模式**：统一使用 `RoundingMode.HALF_UP`（标准四舍五入）
- 各级汇总（goods_amount / service_amount / total_amount）均为已取精度的金额相加，不再二次四舍五入
- Java 中必须使用 `BigDecimal`，禁止 `float` / `double`
- `BigDecimal` 必须用 String 构造（`new BigDecimal("99.00")`），禁止 double 构造
- 金额比较使用 `compareTo()`，不用 `equals()`（`1.0` 与 `1.00` 的 equals 为 false）

**边界条件**：
- SKU 不存在或已下架时需报错
- 库存不足时需拦截（GOODS 类型）
- 会员价未设置时回退到原价

---

### 5. 配送与距离计算模块

**功能点**：
- 系统配置读取（配送半径、起送价、运费策略、接单时段）
- 用户坐标接收（前端选点后传入 lat/lng/address）
- 距离计算（Haversine 或地图服务距离）
- 配送门槛校验：可配送用品原价合计 ≥ 起送价

**业务规则**：
- 购物车中至少有一个 `support_delivery = true` 的用品才显示配送选项；纯服务或全部用品不可配送时不配送
- 起送门槛按**可配送用品的原价合计**判断（不可配送的用品不计入起送价，但仍随单配送）
- 运费策略支持：`FREE`（免运费）/ `TIERED`（分段运费），暂不实现 FIXED 策略
- 分段运费从 0km 开始，要求无重叠、无缺口，且完整覆盖到 deliveryRadiusKm；校验不通过则拒绝保存配置
- 任何情况下均可自提（canDeliver=false 仅影响配送选项显示，不影响自提下单）
- 配送费在下单时自动确定（后端权威计算）

**边界条件**：
- 用户未选择地址但勾选了配送 → 拦截提交
- 用品原价在门槛边界（刚好等于起送价）→ 允许配送
- 超出配送半径 → 不允许配送下单
- 暂不实现接单时段限制，全天可下单（system_config 预留 order_time_enabled/order_start_time/order_end_time 字段，但当前版本不做拦截）

---

### 6. 订单模块

**功能点**：
- 下单（写入 orders + order_item，带价格/手机号/会员等级快照）
- 订单列表查询（C 端按用户、管理端按条件筛选）
- 订单详情查看
- 订单"已处理/未处理"标记切换（管理端操作，用于线下收款后标记）

**业务规则**：
- 订单号格式：日期 + 雪花算法（避免暴露业务量）
- 下单时快照：`customer_phone_snapshot`、`member_level_snapshot`、`original_price`、`deal_price`
- 订单无状态流转（无支付/发货生命周期），仅作为记账记录
- `total_amount = goods_amount + service_amount + delivery_fee`
- 配送费下单时自动计算（按 `system_config` 运费策略：FREE 免运费 / TIERED 分段运费），不存在"待确认"态。~~（≤3km 免费，>3km 按梯度计价表）~~ → **现状**：不再用固定 3km 门槛，统一走配置的策略
- 新订单默认"未处理"，商家线下收款后手动标记为"已处理"

**边界条件**：
- 购物车为空时不可提交
- 并发下单时库存扣减需防超卖（GOODS 类型）
- 下单成功后清空购物车

---

### 7. 通知模块

**功能点**：
- 企业微信群机器人 Webhook 推送新订单通知
- 通知内容格式化（会员标识、商品明细、金额、配送信息、备注）
- 超距订单在通知中标注配送距离和运费
- ~~管理后台 WebSocket + 浏览器 Notification API 兜底推送~~ → **现状**：改用轻量轮询——管理端每 15 秒查 `GET /api/admin/orders/new-count`，有新订单时 `ElNotification` 右下角弹窗 + 自动刷新列表

**业务规则**：
- 下单成功后异步推送，不阻塞下单流程
- 企微机器人每分钟限 20 条（单店完全够用）
- 通知模板含会员等级图标、节省金额提示

**边界条件**：
- Webhook 地址配置错误或网络超时 → 不影响下单，仅日志告警
- ~~WebSocket 断连后需自动重连~~ → **现状**：轮询方案无连接态，无需重连

---

### 8. 后台权限模块（Sa-Token）

**功能点**：
- 多角色管理：BOSS（老板）/ MANAGER（店长）/ STAFF（店员）
- 角色权限矩阵控制接口访问
- 后台账号 CRUD（仅 BOSS 可操作）

**权限矩阵**：
| 功能 | 老板 | 店长 | 店员 |
|------|------|------|------|
| 查看订单 | ✓ | ✓ | ✓ |
| 商品/服务管理 | ✓ | ✓ | ✗ |
| 改价 | ✓ | ✓ | ✗ |
| 会员管理 | ✓ | ✓ | ✗ |
| 会员等级管理 | ✓ | ✓ | ✗ |
| 系统配置 | ✓ | ✗ | ✗ |
| 账号管理 | ✓ | ✗ | ✗ |
| 数据统计 | ✓ | ✓ | ✗ |

---

### 9. 系统配置模块

**功能点**：
- 系统配置读取/更新：`/api/admin/system-config`
- 配送配置：配送半径、起送价、运费策略（免运费/固定/分段）
- 接单配置：接单时段开关、开始/结束时间
- 通知配置：企微 Webhook（前端显示脱敏）
- Webhook 测试发送：`/api/admin/system-config/test-webhook`
- 配置变更记录（操作人、时间、摘要）

**业务规则**：
- 仅 BOSS 角色可修改
- 修改后立即生效

---

### 10. 操作日志模块

**功能点**：
- 记录关键操作（改价、会员等级调整、系统配置修改等）
- 日志字段：操作人、操作类型、目标、修改前后值、时间

---

### 11. 文件上传模块

**功能点**：
- 图片上传（商品封面图）
- 存储在服务器本地目录
- 静态资源通过 Nginx 托管访问

**边界条件**：
- 文件大小限制、格式校验（jpg/png/webp）
- 磁盘空间监控（后期可平滑迁 OSS）

---

### 12. 服务预约系统模块

**功能点**：
- 服务走独立预约入口（首页点主服务「去预约」），不再混入购物车流程
- 预约 = 1 个主服务 SKU + N 个附加服务 SKU + 选开始时间（精确到分钟）
- 全局容量冲突检测：任意时刻同时进行的服务 ≤ 3 个，约满则拒绝
- 实时算价（复用价格引擎，会员按等级折扣）+ 选时间时的时段可约状态预检
- 预约状态流转：PENDING（待服务）→ SERVICED（已完成）/ CANCELLED（已取消，时段释放）
- C 端「我的预约」列表 + 取消；Admin 预约看板（按日/状态/关键词）+ 标记完成 + 取消
- 预约下单复用订单体系（`orders` + `order_item`），额外挂一条 `appointment` 记录；通知带预约时间、总时长、宠物信息

**业务规则**：
- `product.service_category` 区分主服务（`MAIN_SERVICE`）与附加服务（`ADDON_SERVICE`）；仅主服务可发起预约
- 总占用时长 = 主服务 SKU `duration` + Σ 附加服务 SKU `duration`（附加服务 `duration` 缺省 0，表示只加钱不占时间）
- 冲突算法采用区间重叠计数：统计与新预约 `[start, end)` 重叠且非取消的已有预约数，≥ 3 即满；应用层检查即可，单店低流量不加 DB 锁
- 营业时间：`system_config.order_time_enabled = 1` 时，预约开始时间必须落在 `[order_start_time, order_end_time)` 内（仅约束开始时间；实物商品下单不受此字段约束）
- 取消预约联动订单 `cancelled = 1`，商家后台据此识别；时段随之释放，可被他人预约

**边界条件**：
- 主服务 SKU 未配 `duration`（≤0 或 NULL）→ 拒绝预约，提示「联系店主配置」
- 仅 PENDING 状态可取消或标记完成；SERVICED 不可取消
- C 端跨账号取消需校验归属，只能取消自己的预约

**接口**：见 `api.md`「预约模块」C 端 6 个 + 管理端 3 个。

---

## 二、前端模块

### A. 用户端 H5（Vue 3 + Vant 4 + 腾讯地图）

#### H5-1. 登录页

**功能点**：
- 手机号输入（11 位校验）
- 验证码输入 + 发送按钮（60s 倒计时）
- 登录态管理：Sa-Token 默认方案，token 存 HttpOnly cookie，前端无需手动管理，每次请求自动续期

**交互细节**（参考 MVP）：
- 首次扫码 → 弹登录遮罩层
- 已登录 → 直接进入首页
- 顶部栏显示当前手机号（脱敏）+ 会员等级信息

---

#### H5-2. 首页（商品浏览）

**功能点**：
- 顶部导航栏：店铺名称 + 会员等级徽章 + 订单入口
- 搜索栏：圆角搜索框，前端本地过滤（同时搜索商品和服务）
- 左侧：类型导航（「商品」/「服务」两项）
- 右侧：商品列表（封面图 + 名称 + 价格展示）
- 底部：购物车悬浮栏（件数 + 金额 + 查看购物车按钮）

**交互细节**：
- 页面加载时一次性拉取全部商品（商品 + 服务），按侧边栏类型过滤展示
- 搜索时跨类型过滤，结果同时包含商品和服务
- 非会员：只显示原价
- 会员：显示原价（划线灰色）+ 成交价
- 无规格商品：直接 +/- 按钮操作
- 有规格商品：显示「选规格」按钮 + 价格后带「起」字（如 ¥72.00 起）
- 商品卡片不显示「配送」/「到店」标签

---

#### H5-3. SKU 选择弹窗

**功能点**：
- 商品/服务名称展示
- 规格列表展示（名称 + 价格）
- 数量选择（+/-）
- 「加入购物车」按钮

**交互细节**：
- 弹窗从底部弹出（自定义组件）
- 默认选中第一个规格
- 会员价/折扣价实时计算展示
- 不展示库存信息
- 商品描述区域：在商品名/价格下方展示 `description`（2 行省略）；首页商品卡片同样在商品名下展示描述（单行省略）

---

#### H5-4. 购物车页

**功能点**：
- 购物车商品列表（名称/规格 + 单价 + 数量 +/-）
- 实时金额计算（含会员价）
- 会员提示：「XX档会员 · 服务享X折 · 商品享会员价」
- 空购物车提示
- 「去结算」按钮

**交互细节**：
- 删除商品：数量减至 0 自动移除
- 金额实时更新

---

#### H5-5. 结算/确认订单页

**功能点**：
- 联系人姓名输入（可选）
- 备注输入（可选）
- 配送开关（仅含可配送用品时显示，可配送用品原价 ≥ ¥20 才能勾选）
- 腾讯地图选点（勾选配送后弹出）
- 费用明细展示（商品金额 + 配送费 + 合计）
- 提交订单按钮

**交互细节**（参考 MVP）：
- 配送开关逻辑：
  - 无可配送用品（纯服务 / 所有用品均不可配送）→ 隐藏开关，提示「当前商品不支持配送」
  - 可配送用品原价 < ¥20 → 开关禁用，提示「还差 ¥X 起送」
  - 可配送用品原价 ≥ ¥20 → 可勾选
- 勾选配送后弹出腾讯地图选点组件：
  - 默认定位用户当前位置
  - 支持拖动大头针 / 搜索关键词
  - 选点后自动逆地址解析填入详细地址
- 非会员结算页提示「您不是会员，已按原价结算」

---

#### H5-6. 下单成功页

**功能点**：
- 订单号展示
- 待付金额展示（非"支付金额"）
- 线下付款提示：「请与店主通过其他方式完成付款」
- 超距提示（如适用）
- 「继续选购」+「查看订单」两个按钮

---

#### H5-7. 预约页（服务）

**功能点**：
- 独立预约入口 `/appointment/:productId`，仅主服务（`serviceCategory=MAIN_SERVICE`）进入
- 宠物信息文本填写（名字/种类/体重/性格等，不建宠物档案表）
- 主服务 SKU 选择（带 `duration` 时长展示）
- 附加服务多选（按主服务在 `main_service_addon` 中绑定的列表）
- 选开始时间：按营业时段生成半小时步进的时间网格，约满时段置灰
- 实时算价 + 冲突预检（选时间即查可约状态）
- 提交 → 生成订单 + 预约，订单详情/成功页展示预约时间与总时长

**交互细节**：
- 首页主服务卡片按钮显示「去预约」，点击跳转预约页（替代加购）
- 实物商品（GOODS）购物车流程不受影响
- 预约信息以备注形式写入订单（`预约 yyyy-MM-dd HH:mm（N分钟）；宠物：…`），订单详情、Admin、通知均可见

---

#### H5-8. 我的预约

**功能点**：
- 按状态筛选（待服务/已完成/已取消）查看自己的预约
- PENDING 状态可取消，取消后时段释放
- 预约详情展示主服务、附加服务、时间、宠物信息

---

### B. 管理端（Vue 3 + Element Plus）

#### Admin-1. 登录页

**功能点**：
- 用户名 + 密码登录
- 登录后根据角色显示对应菜单

---

#### Admin-2. 商品管理

**功能点**：
- 商品列表（搜索、筛选、上下架操作）
- 新增/编辑商品（名称、~~分类~~描述、封面图、类型、排序）
- SKU 管理（规格名、原价、会员价、库存、排序）
- ~~分类管理（名称、图标、排序、类型）~~ → **现状**：分类子分类已下线，无此页

---

#### Admin-3. 会员管理

**功能点**：
- 会员列表（搜索、按等级筛选）
- 新增/编辑会员（姓名、手机号、等级、备注）
- 会员等级管理（等级名、折扣率、排序、启停用）

**权限**：
- 会员信息管理：老板 + 店长
- 会员等级管理：仅老板

---

#### Admin-4. 订单管理

**功能点**：
- 订单列表（按时间、手机号、会员、处理状态等筛选）
- 订单详情查看（商品明细、金额、配送信息、备注）
- 订单"已处理/未处理"标记切换
- ~~WebSocket 实时新订单推送 + 浏览器弹窗通知~~ → **现状**：15 秒轮询 `GET /api/admin/orders/new-count` + ElNotification 右下角弹窗 + 自动刷新列表

**权限**：
- 所有角色可查看订单
- 标记"已处理"：老板 + 店长 + 店员

---

#### Admin-5. 系统配置

**功能点**：
- 店铺坐标设置（经纬度，地图选点）
- 配送规则设置（配送半径、起送金额、运费策略 FREE/TIERED + 分段规则）
- 企微机器人 Webhook 地址配置

**权限**：仅老板

---

#### Admin-6. 账号管理

**功能点**：
- 后台账号 CRUD（用户名、密码、姓名、角色、状态）
- 启用/禁用账号

**权限**：仅老板

---

#### Admin-7. 数据统计

**功能点**：
- 订单量统计（按日/周/月）
- 销售额统计
- 会员消费排行

**权限**：老板 + 店长

---

#### Admin-8. 操作日志

**功能点**：
- 操作日志列表（按时间、操作人、操作类型筛选）
- 日志详情查看（修改前后值对比）

---

#### Admin-9. 预约看板

**功能点**：
- 预约列表按日（预约开始时间）+ 状态 + 关键词筛选
- 标记预约完成（PENDING → SERVICED）
- 取消预约（联动订单 `cancelled = 1`）
- 商品管理中配置 `service_category`（主服务/附加服务）与 SKU `duration`
- 主服务-附加服务绑定管理
- 营业时段配置（复用系统配置 `order_time_enabled/start/end_time`）

**权限**：
- 看板查看：所有角色
- 标记完成/取消：BOSS + MANAGER

---

## 三、数据库设计

> 所有表均包含 `id`（BIGINT 自增主键）、`create_time`、`update_time` 三个必备字段。更新操作必须同步更新 `update_time`。

### 商品体系

> ~~`category` 商品分类表~~ → **现状**：分类子分类已下线，该表不再使用（仅保留 GOODS/SERVICE 两类，由 `product.type` 区分）。

```sql
-- 商品/服务（统一表，type 区分）
CREATE TABLE product (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  name              VARCHAR(128) NOT NULL,
  description       TEXT         NULL,
  cover_img         VARCHAR(255) NULL,
  type              VARCHAR(16)  NOT NULL COMMENT 'GOODS / SERVICE',
  service_category  VARCHAR(16)  NULL COMMENT '服务子类 MAIN_SERVICE/ADDON_SERVICE，仅 type=SERVICE 有效',
  status            VARCHAR(16)  NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE / OFF_SALE',
  support_delivery  TINYINT      NOT NULL DEFAULT 0 COMMENT '仅 GOODS 有效，SERVICE 强制为 0',
  sort              INT          NOT NULL DEFAULT 0,
  create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_product_type_status(type, status),
  INDEX idx_product_service_category(type, service_category, status)
);

-- SKU（多规格）
CREATE TABLE sku (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id    BIGINT        NOT NULL,
  spec_name     VARCHAR(64)   NOT NULL COMMENT '如 "5kg" / "中型犬（10-25kg）"',
  price         DECIMAL(10,2) NOT NULL COMMENT '原价',
  member_price  DECIMAL(10,2) NULL COMMENT '会员价（仅 GOODS 用，SERVICE 忽略）',
  duration      INT           NULL COMMENT '服务时长（分钟），仅 SERVICE 的 SKU 用，GOODS 为 NULL',
  stock         INT           NOT NULL DEFAULT 0 COMMENT 'SERVICE 可为 -1 表示不限',
  sort          INT           NOT NULL DEFAULT 0,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_sku_product(product_id)
);
```

### 会员体系

```sql
-- C 端用户账号（手机号验证码登录）
CREATE TABLE app_user (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  phone           VARCHAR(16) NOT NULL COMMENT '登录手机号',
  status          TINYINT     NOT NULL DEFAULT 1 COMMENT '0 禁用 / 1 正常',
  last_login_time DATETIME    NULL,
  create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX uk_phone(phone)
);

-- 会员等级
CREATE TABLE member_level (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(32)   NOT NULL COMMENT '如 "500档会员"',
  discount_rate DECIMAL(5,4)  NOT NULL COMMENT '服务折扣率，0.85 = 8.5折',
  sort          INT           NOT NULL DEFAULT 0,
  status        TINYINT       NOT NULL DEFAULT 1 COMMENT '0 停用 / 1 启用',
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 会员
CREATE TABLE member (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(32)  NOT NULL,
  level_id     BIGINT       NOT NULL,
  remark       VARCHAR(255) NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_member_level(level_id)
);

-- 会员手机号映射（一个会员可绑定多个手机号）
CREATE TABLE member_phone (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id    BIGINT       NOT NULL,
  phone        VARCHAR(16)  NOT NULL COMMENT '与 app_user.phone 对应',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX uk_member_phone(phone),
  INDEX idx_member_phone_member(member_id)
);
```

### 订单

```sql
-- 订单主表
CREATE TABLE orders (
  id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no               VARCHAR(32)   NOT NULL COMMENT '日期+雪花算法',
  user_id                BIGINT        NOT NULL COMMENT 'app_user.id',
  customer_phone_snapshot VARCHAR(16)  NOT NULL COMMENT '下单时手机号快照',
  customer_name          VARCHAR(32)   NULL COMMENT '联系人，可选',
  member_id              BIGINT        NULL COMMENT '命中会员则记录',
  member_level_snapshot  VARCHAR(32)   NULL COMMENT '下单时等级名快照',
  goods_amount           DECIMAL(10,2) NOT NULL COMMENT '用品金额（已折扣）',
  service_amount         DECIMAL(10,2) NOT NULL COMMENT '服务金额（已折扣）',
  delivery_fee           DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '配送费（按 system_config 运费策略 FREE/TIERED 计算）',
  total_amount           DECIMAL(10,2) NOT NULL COMMENT 'goods + service + delivery_fee',
  need_delivery          TINYINT       NOT NULL DEFAULT 0,
  delivery_address       VARCHAR(255)  NULL,
  delivery_lat           DECIMAL(10,7) NULL,
  delivery_lng           DECIMAL(10,7) NULL,
  delivery_distance      INT           NULL COMMENT '与店铺直线距离，单位米',
  processed              TINYINT       NOT NULL DEFAULT 0 COMMENT '0 未处理 / 1 已处理（管理端标记）',
  cancelled              TINYINT       NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 已取消（取消预约时联动置 1，商家据此识别）',
  remark                 VARCHAR(255)  NULL,
  create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX uk_order_no(order_no),
  INDEX idx_orders_user(user_id),
  INDEX idx_orders_member(member_id),
  INDEX idx_orders_create_time(create_time),
  INDEX idx_orders_processed(processed)
);

-- 订单明细
CREATE TABLE order_item (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id       BIGINT        NOT NULL,
  product_id     BIGINT        NOT NULL,
  sku_id         BIGINT        NULL,
  type           VARCHAR(16)   NOT NULL COMMENT 'GOODS / SERVICE',
  product_name   VARCHAR(128)  NOT NULL COMMENT '下单时快照',
  sku_name       VARCHAR(64)   NULL COMMENT '下单时快照',
  original_price DECIMAL(10,2) NOT NULL COMMENT '原价快照',
  deal_price     DECIMAL(10,2) NOT NULL COMMENT '成交价快照（已含会员价/折扣）',
  quantity       INT           NOT NULL,
  subtotal       DECIMAL(10,2) NOT NULL COMMENT 'deal_price × quantity',
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_order_item_order(order_id)
);
```

### 预约体系

```sql
-- 主服务与附加服务的绑定关系（每个主服务单独绑定可选附加服务）
CREATE TABLE main_service_addon (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  main_product_id  BIGINT NOT NULL COMMENT '主服务 product.id（service_category=MAIN_SERVICE）',
  addon_product_id BIGINT NOT NULL COMMENT '附加服务 product.id（service_category=ADDON_SERVICE）',
  sort             INT    NOT NULL DEFAULT 0,
  create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX uk_main_addon(main_product_id, addon_product_id),
  INDEX idx_addon_main(addon_product_id)
);

-- 预约记录（与 orders 一对一，order_id 关联）
CREATE TABLE appointment (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id        BIGINT      NOT NULL COMMENT '关联 orders.id（一笔预约对应一个订单）',
  user_id         BIGINT      NOT NULL COMMENT 'app_user.id',
  main_product_id BIGINT      NOT NULL COMMENT '主服务 product.id 快照',
  main_sku_id     BIGINT      NOT NULL COMMENT '主服务 sku.id 快照',
  start_time      DATETIME    NOT NULL COMMENT '预约开始时间（顾客到店/服务开始）',
  end_time        DATETIME    NOT NULL COMMENT 'start_time + total_duration 分钟',
  total_duration  INT         NOT NULL COMMENT '总占用时长（分钟）= 主 + Σ附加',
  pet_info        VARCHAR(255) NULL COMMENT '宠物信息（文本：名字/种类/体重/性格等）',
  status          VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待服务 / SERVICED 已完成 / CANCELLED 已取消',
  create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX uk_appointment_order(order_id),
  INDEX idx_appointment_user(user_id),
  INDEX idx_appointment_status_time(status, start_time, end_time),
  INDEX idx_appointment_time_range(start_time, end_time)
);
```

### 系统

```sql
-- 后台账号
CREATE TABLE admin_user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(32)  NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name   VARCHAR(32)  NOT NULL,
  role        VARCHAR(16)  NOT NULL COMMENT 'BOSS / MANAGER / STAFF',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0 禁用 / 1 启用',
  last_login_time DATETIME NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE INDEX uk_username(username)
);

-- 操作日志
CREATE TABLE operation_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT       NOT NULL,
  action      VARCHAR(32)  NOT NULL COMMENT '如 UPDATE_PRODUCT / UPDATE_CONFIG',
  target      VARCHAR(128) NOT NULL COMMENT '如 "商品:金毛粮5kg"',
  before_val  TEXT         NULL COMMENT '修改前值 JSON',
  after_val   TEXT         NULL COMMENT '修改后值 JSON',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_log_time(create_time),
  INDEX idx_log_user(user_id)
);

-- 系统配置（单行表，ID=1）
CREATE TABLE system_config (
  id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
  shop_lat              DECIMAL(10,7)  NULL COMMENT '店铺纬度（用于 Haversine 距离计算）',
  shop_lng              DECIMAL(10,7)  NULL COMMENT '店铺经度',
  delivery_radius_km    DECIMAL(6,2)   NOT NULL,
  delivery_min_amount   DECIMAL(10,2)  NOT NULL,
  delivery_fee_type     VARCHAR(16)    NOT NULL COMMENT 'FREE / TIERED',
  fixed_delivery_fee    DECIMAL(10,2)  NOT NULL DEFAULT 0 COMMENT '保留字段（当前不使用）',
  order_time_enabled    TINYINT        NOT NULL DEFAULT 0 COMMENT '预留：接单时段开关',
  order_start_time      TIME           NULL COMMENT '预留：接单开始时间',
  order_end_time        TIME           NULL COMMENT '预留：接单结束时间',
  qywx_webhook_url_enc  VARBINARY(1024) NULL COMMENT '加密存储',
  has_qywx_webhook      TINYINT        NOT NULL DEFAULT 0,
  updated_by            BIGINT         NULL COMMENT 'admin_user.id',
  create_time           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 分段运费配置
CREATE TABLE system_config_delivery_tier (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_id        BIGINT        NOT NULL,
  min_distance_km  DECIMAL(6,2)  NOT NULL,
  max_distance_km  DECIMAL(6,2)  NOT NULL,
  fee              DECIMAL(10,2) NOT NULL,
  sort             INT           NOT NULL DEFAULT 0,
  create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_tier_config(config_id, sort)
);

-- 系统配置变更日志
CREATE TABLE system_config_log (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_id     BIGINT        NOT NULL,
  operator_id   BIGINT        NOT NULL COMMENT 'admin_user.id',
  operator_name VARCHAR(32)   NOT NULL,
  summary       VARCHAR(255)  NOT NULL,
  before_val    JSON          NULL,
  after_val     JSON          NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_config_log_config(config_id),
  INDEX idx_config_log_time(create_time)
);
```

### 配送计价规则

| 策略 | 运费 | 现状 |
|------|------|------|
| FREE | 始终免运费 | ✅ 已实现 |
| FIXED | 按固定金额收取 | ❌ 未实现（后端 `SystemConfigServiceImpl` 仅接受 FREE/TIERED） |
| TIERED | 按 `system_config_delivery_tier` 距离分段自动计算，下单时确定 | ✅ 已实现 |

---

## 四、模块依赖关系（开发顺序参考）

```
后端基础层（先开发）：
  认证与登录 → 系统配置 → 文件上传 → 商品

后端业务层（依赖基础层）：
  会员模块 → 价格计算引擎 → 配送与距离计算 → 订单模块 → 通知模块

前端（依赖后端接口）：
  H5 登录 → H5 首页/商品浏览 → H5 购物车 → H5 结算 → H5 下单成功
  Admin 登录 → Admin 商品管理 → Admin 会员管理 → Admin 订单管理 → Admin 其他
```

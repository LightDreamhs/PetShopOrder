# PetShopOrder 开发进度

> 更新时间：2026-07-03

## 项目概况

宠物店点单系统，顾客(H5)浏览商品下单，商家(Admin)管理商品/会员/订单。

**技术栈：** Spring Boot 3.3.6 / Vue 3 + TypeScript / Vant 4(H5) / Element Plus(Admin) / MySQL 8.0 / Sa-Token / 腾讯地图 JS API GL

## 后端 — 已完成

| Phase | 模块 | 说明 |
|-------|------|------|
| 0 | 项目骨架 | Docker MySQL + 基础框架 |
| 1 | 认证 | C 端短信登录 + Admin 账密登录（Sa-Token cookie 会话） |
| 2 | 文件上传 | 本地存储，Admin 上传/删除 |
| 3 | 商品 | 商品 + SKU CRUD，类型 GOODS/SERVICE，已移除 category 子分类 |
| 4 | 会员 | 等级 CRUD + 会员多手机号绑定 + C 端身份匹配 |
| 5 | 价格计算 | GOODS 用会员价 / SERVICE 用等级折扣率（BigDecimal HALF_UP） |
| 6 | 配送 | Haversine 距离 + 起送价校验 + 分段运费 |
| 7 | 系统配置 | 配置读写 + 变更审计日志 + Webhook SSRF 防护 + 收款二维码 |
| 8 | 订单 | 创建/查询/快照，下单时自动触发通知；结算页备注 placeholder 改为「在此预约服务时间」（临时引导，待预约系统上线后还原） |
| 9 | 通知 | 飞书/企微 Webhook 异步推送 + 失败重试 |
| 10 | Admin 用户 | CRUD + BOSS 保护 + BCrypt |
| 11 | 操作日志 | 分页查询 |
| 12 | 数据统计 | 总览/趋势/排行 |

## 前端 Admin — 已完成

| 模块 | 说明 |
|------|------|
| 登录 | 深色背景 + 角色鉴权路由守卫 |
| 订单管理 | 筛选 + 详情抽屉 + 标记已处理 + **新订单轮询通知（15s 间隔，右下角弹窗"有新订单来了"）** |
| 商品管理 | 商品 CRUD + SKU + 上下架（已移除分类管理） |
| 会员管理 | 会员列表 + 等级 CRUD + 多手机号 |
| 系统配置 | 配送/接单/通知配置 + 收款二维码上传 + **店铺坐标地图选点（腾讯地图 GL + libraries=service）** + 变更记录 |
| 账号管理 | CRUD + 启停用 + 重置密码（仅 BOSS） |

## 前端 H5 — 已完成

| 模块 | 说明 |
|------|------|
| 登录 | 手机号 + 验证码（固定 1234） |
| 首页 | 「用品」/「服务」Tab 切换 + 搜索 + SKU 弹窗 + CartBar |
| 购物车 | 纯前端 Pinia + localStorage |
| 结算 | 商品明细 + 会员折扣 + 配送开关 + **腾讯地图选点** + **常用地址管理（列表选择/设默认/删除）** |
| 下单 | 提交订单 → 成功页（展示收款码，260×260，显示"应付金额"） |
| 订单列表/详情 | 分页加载 + 状态展示 |
| 店铺 Logo | 首页导航栏 + 登录页 Logo 由 emoji（🐾）替换为真实店铺 logo 图片（`assets/shop-logo.jpg`） |

## 前后端联调 — 已通过（2026-05-26）

| 步骤 | 结果 |
|------|------|
| 商家登录 Admin | ✅ |
| 配置系统参数 | ✅ |
| 新增商品 + SKU（H5 可见） | ✅ |
| 会员等级管理 | ✅ |
| 录入会员 + 手机号绑定 | ✅ |
| 顾客 H5 登录 + 会员徽章 | ✅ |
| 浏览商品（Tab 切换） | ✅ |
| 加购物车 | ✅ |
| 购物车 → 结算（金额计算） | ✅ |
| 提交订单（Admin 可见） | ✅ |
| 查看订单 + 订单详情 | ✅ |
| Admin 标记已处理（H5 同步） | ✅ |
| 商品上下架 | ✅ |
| 会员折扣变更 | ✅ |

## Bug 修复记录（2026-06-03）

| 问题 | 根因 | 修复 |
|------|------|------|
| 系统配置保存报"服务器内部错误" | AES 密钥 `PetShop2026Order!` 17 字节，AES 要求 16/24/32 | 所有 AES 调用统一改用 SHA-256 派生 32 字节密钥 |
| 收款码上传后不显示 | Mapper XML 缺 `payment_qr_url`、Controller Request/Params 未传递该字段 | 三处补全：Mapper select/update/insert、Request 类、buildUpdateParams |
| Admin 菜单高亮与未保存提醒不同步 | `el-menu` 点击立即更新内部状态，导航被取消后不恢复 | `handleMenuSelect` 改 async，导航完成后 `menuKey++` 强制 el-menu 重新渲染 |
| 地图选点报"加载失败" | 腾讯地图 GLJS 未加 `libraries=service`，`TMap.service.Geocoder` 为 undefined | URL 加 `&libraries=service`，就绪检查同时等待 `TMap.service.Geocoder` |
| 地图拖动后地址不更新 | 事件名 `map_move_end` 不是腾讯地图 GL 的正确事件名 | 改为 `moveend` |
| 地图搜索无结果 | `TMap.service.Suggestion.search()` 和 `Search.search()` 均不存在 | 改用 `Search.searchRegion({ keyword, cityName: '全国' })` |
| el-dialog 打开地图白屏 | `@open` 时 DOM 未渲染完成，地图容器尺寸为 0 | 改为 `@opened`（对话框动画结束后初始化） |
| 系统配置页白屏 | `@element-plus/icons-vue` 无 `Circle` 导出，导致 MapLocationPicker 加载失败 | 用 CSS 空心圆替代 |
| el-radio 降级警告 | `el-radio-button` 使用 `:label` 传值，新版 Element Plus 要求 `:value` | `:label` → `:value` |
| .env 文件被覆盖 | 创建 .env 时 `echo >` 覆盖了原 `VITE_API_BASE_URL=/` | 恢复原内容 + 追加 `VITE_TMAP_KEY` |
| 地图 UI 重构 | 原设计为全屏地图+搜索浮层，体验不佳 | 参考美团风格：上方地图(40vh)+下方可滚动 POI 列表，拖动地图自动刷新附近 POI（`getPoi:true`），点击列表项联动地图跳转 |
| Webhook 通知发送失败 | `NotificationServiceImpl` 也用了原始 `aesKey.getBytes()` | 统一改为 `deriveAesKey()` |
| 通知消息时间不显示 | `order.createTime` 在 insert 后 Java 对象中为 null（数据库 DEFAULT 填充） | 为 null 时用 `LocalDateTime.now()` 兜底 |
| 通知消息缺少信息 | 无联系人、备注、地址；电话脱敏；商品不换行 | 电话不脱敏、商品逐行加粗、新增联系人/备注/配送地址 |

## 前后端联调期间优化（2026-06-21）

联调测试中发现并修复的问题与体验优化。

| 类别 | 改动 | 涉及文件 | 说明 |
|------|------|---------|------|
| 功能补全 | 收货地址补全 detail（楼号/门牌号）字段 | `AddressPicker.vue` `CheckoutPage.vue` `AddressManagePage.vue` | 后端/DB 早已有 `detail` 字段，但前端无输入入口。地图选点加「门牌号」输入框；地址管理页加「编辑门牌号」入口；结算页地址分行展示，**下单时拼接成「POI名称 门牌号」存入订单快照**，订单详情/Admin/Webhook 自动带完整地址 |
| Bug 修复 | 结算页跳地址管理后返回，配送开关/备注/联系人状态丢失 | `App.vue`（新增）`stores/keepAlive.ts` `CheckoutPage.vue` `stores/auth.ts` | 原 `App.vue` 为裸 `router-view` 无 keep-alive，结算页跳路由即被销毁。改用 `keep-alive include="Checkout"` 仅缓存结算页；`onActivated` 返回时重拉地址并同步已选地址（detail/删除/默认）；下单成功 `resetDraft()+dropCheckout()` 清草稿与缓存；登出也清缓存防换账号残留 |
| Bug 修复 | Admin 新订单轮询通知不弹（时区错配）⭐ | `backend/docker-compose.yml` | **根因**：MySQL 容器默认 UTC 时区，订单 `create_time` 按 UTC 落库（慢 8 小时）；前端 `formatNow()` 用浏览器北京时间做 `since`。比较 `create_time > since` 恒为假 → count=0 → 永不弹通知。docker-compose 加 `TZ=Asia/Shanghai` + `--default-time-zone=+08:00`，重启容器后 `NOW()` 为北京时间，与新订单写入时区一致。注：历史测试单 create_time 为 UTC 脏数据，仅影响旧单，新单正常 |
| UI 优化 | 地址管理页卡片增强 + 删除确认框重做 | `AddressManagePage.vue` | 默认地址金色高亮卡片+「默认地址」橙标；定位图标；操作按钮分主次；删除从 Vant 默认白板弹窗改为**底部弹出确认条**，显示完整地址（POI+门牌分两行）+ 红色危险按钮 |
| UI 优化 | 首页右上角「订单」按钮 →「我的」下拉菜单 | `HomePage.vue` | 「订单」按钮替换为「我的」下拉：含「我的订单」「我的地址」（地址管理页）两项，箭头旋转动画，点外部自动收起 |

> 注：`vue-tsc` 报 `AddressPicker.vue:62` 的 `van-tag :type` 类型错误为改动前已存在问题，与本次改动无关，不影响 dev 运行。
>
> 产品决策：H5 顾客端「我的订单」不显示订单处理状态（商家内部状态，Admin 端可见即可），类型定义无 `processed` 字段，保持 C 端简洁。

## 文档核对与修订（2026-06-30）

逐份核对项目文档与代码实现，修订过时内容（验证：前后端 build + 后端编译均通过）。

| 文档 | 修订内容 |
|------|---------|
| `api.md` | 删除已废弃的「分类管理」整章（4 个 CRUD 接口）、C 端 categories 接口段；删除未实现的 WebSocket 章节，总览 WS 行替换为实际轮询接口 `GET /api/admin/orders/new-count`；清理商品接口残留的 `categoryId`/`categoryName` 字段；商品列表/详情响应补 `description` 字段；管理端章节重新编号；顶部加修订说明 |
| `modules.md` | 以「现状」标注修订：分类子分类下线、WebSocket 改 15s 轮询、FIXED 未实现、3km 门槛改为配置策略、删除 `category` 表 DDL 与 `product.category_id`；H5-3 描述区改为已实现；顶部加修订说明 |
| `plan.md` | 顶部加「历史档案」警示，列出 6 项已被推翻的决策（分类、3km/OVER_RANGE、FIXED、WebSocket、企微→飞书、短信自研→PNVS），指向权威文档 |

### 待处理（尚未动，待确认）

- `AGENTS.md` 与 `CLAUDE.md` 内容完全重复，建议保留其一。
- `飞书订单通知群webhook.txt` 含真实 webhook 明文地址且疑似未在 `.gitignore`，有泄露风险，建议移出仓库并加忽略。

## 短信认证服务接入（PNVS）（2026-06-30）

按 [`deploy/sms-pnvs-planning/实施计划.md`](deploy/sms-pnvs-planning/实施计划.md) 落地阿里云号码认证服务（PNVS），改造 H5 登录从"内存固定 `1234`"为"可切换真实核验"。**前端 `LoginPage.vue` 零改动**（接口签名不变）。`mvn compile` 通过。

### 改动清单（7 处）

| 操作 | 文件 | 说明 |
|------|------|------|
| 改 | `backend/pom.xml` | 加 SDK `com.aliyun:dypnsapi20170525:2.0.0`（**同步 SDK**，计划写的是异步 `alibabacloud-d` 前缀，按低耦合/阻塞契合原则改用同步） |
| 改 | `backend/src/main/resources/application.yml` | 新增 `sms` 命名空间，默认 `provider=log`，AK/SK 走环境变量，签名/模板已填默认值 |
| 新建 | `backend/.../config/SmsProperties.java` | `@ConfigurationProperties(prefix="sms")` + `@Component` 自注册（无需改主启动类） |
| 新建 | `backend/.../sms/SmsVerifyService.java` | 接口 `send(phone)` + `verify(phone, code)` |
| 新建 | `backend/.../sms/LogSmsService.java` | `provider=log`（默认）兜底，`1234` 放行 |
| 新建 | `backend/.../sms/AliyunPnvsSmsService.java` | `provider=aliyun` 真实核验（`SendSmsVerifyCode` + `CheckSmsVerifyCode`） |
| 改 | `backend/.../service/impl/AppAuthServiceImpl.java` | 删 `codeStore` + 固定码，注入 `SmsVerifyService` |

### 核对官方文档修正的坑（避免上线翻车）

- `CodeType`/`CodeLength` setter 入参是 `Long` 不是 `int` → 传 `1L`/`6L`
- `verifyResult` **嵌套在 `Body.Model` 内**，不在顶层 Body → `getBody().getModel().getVerifyResult()`
- 发送成功判定是 `Code="OK"` **不是 `"200"`**
- `TemplateParam` 必填 `{"code":"##code##","min":"5"}`，否则阿里云不生成验证码，`CheckSmsVerifyCode` 必失败

### 切换真实短信

默认 `provider=log`，`1234` 仍可登录，联调不受影响。真实核验需设环境变量（无需改代码/重新编译）：

```bash
export SMS_PROVIDER=aliyun
export ALIYUN_SMS_AK=<AccessKey ID>
export ALIYUN_SMS_SK=<AccessKey Secret>
# sign-name=速通互联验证码、template-code=100001 已在 yml 设默认值
```

### 待办

- [ ] 真实手机号端到端验证（`provider=aliyun`：发码→收码→正/误码登录）
- [ ] `短信验证服务Access key.txt` 现含明文 Secret，仅本地 `.git/info/exclude` 忽略；建议另存密码管理器，避免单点丢失

## 已知问题 & 待开发

| 优先级 | 项目 | 说明 |
|--------|------|------|
| ~~高~~ | ~~地址选择器~~ | ✅ 已完成（2026-06-01）：接入腾讯地图 JS API GL，H5 地图选点+搜索+定位，Admin 店铺坐标配置 |
| ~~中~~ | ~~用户收货地址管理~~ | ✅ 已完成（2026-06-04）：新增 `user_address` 表 + CRUD 接口，CheckoutPage 改为地址列表选择（省地图 API 调用），AddressPicker 增加保存常用地址+标签，新增地址管理页（设默认/删除） |
| ~~高~~ | ~~起送金额硬编码~~ | ✅ 已修复（2026-06-01）：改用 calculate 接口返回的 `deliveryCheck.reachedMinAmount` / `gap` |
| 中 | 购物车改为底部弹窗 | CartBar 点购物车图标应弹出底部抽屉而非跳转页面 |
| 中 | ~~下单成功页加收款码~~ | ✅ 已完成（2026-06-01），二维码已放大至 260×260 |
| ~~中~~ | ~~配送规则联调~~ | ✅ 已完成（2026-06-03）：收款码持久化、地图选点、通知推送全链路联调通过 |
| ~~低~~ | ~~短信验证码~~ | ✅ 已接入阿里云号码认证服务（PNVS）（2026-06-30）：默认 `provider=log` 兜底（`1234` 仍可用），真实核验待环境变量切换 + 端到端验证，详见下文「短信认证服务接入」段 |
| 低 | Admin 数据统计页 | 订单趋势、会员排行（后端接口已有） |
| 低 | Admin 操作日志页 | 后端接口已有，前端未挂路由 |
| ~~低~~ | ~~WebSocket~~ | ✅ 已用轻量轮询方案替代（2026-06-03）：15s 间隔查询新订单计数 + ElNotification 弹窗提醒 + 自动刷新列表 |
| ~~中~~ | ~~H5 商品简介（描述）展示~~ | ✅ 已完成（2026-06-30）：后端 `GET /api/app/products` 列表接口 `toAppMap()` 回传 `description`；H5 首页 `ProductCard` 商品名下加描述（单行省略）、SKU 弹窗 `SkuSelectorPopup` 填充原预留 `.sku-desc` 占位（2 行省略）；`Product` 类型补字段。 |
| 🔴 高 | 服务预约系统 | 📋 **方案已定稿待执行（2026-07-03）**。当前服务像商品一样直接下单、无时间维度。新方案：**主服务+附加服务+选时间**的独立预约入口，带**时间冲突检测**（任意时刻同时进行 ≤3 个）。完整方案见 [`docs/appointment-system-plan.md`](docs/appointment-system-plan.md)，分 4 期实施，第 1 期（数据层+后端核心闭环）即可形成最小可用闭环。 |

## 服务预约系统（规划中，2026-07-03）

> 📋 **状态：方案已定稿，待分期执行**。完整方案见 [`docs/appointment-system-plan.md`](docs/appointment-system-plan.md)。

### 背景

当前系统把服务（`type=SERVICE`）当成"不可配送的特殊商品"来卖——下单不选时间、不选服务人员，时间安排只能塞进订单备注自由文本，**无法校验时间冲突**。需新增"主服务+附加服务+选时间"的预约能力，并解决多预约之间的时间冲突。

### 核心约束（已与产品方逐条确认）

| 维度 | 决策 |
|---|---|
| 全局容量 | 任意时刻同时进行的服务 **≤ 3 个**（留余量） |
| 资源模型 | 不区分美容师/工位，只数全局同时进行数 |
| 冲突算法 | 区间重叠计数：新预约 `[start, start+总时长)` 与已有非取消预约比较，重叠数 ≥3 拒绝 |
| 并发安全 | 应用层检查即可（单店低流量，极端竞态事后人工处理，不加 DB 锁） |
| 服务时长 | 按 SKU 配 `duration`（分钟），店主自填 |
| 总占用时长 | 主服务 duration + Σ 附加服务 duration（累加） |
| 时间粒度 | 精确任意时间（如 09:30），用户自选开始时间 |
| 下单模式 | 1 主服务 + N 附加服务，主服务占时间段，附加服务跟着走 |
| 主/附加区分 | `product.service_category`（`MAIN_SERVICE` / `ADDON_SERVICE`） |
| 附加服务范围 | 每个主服务单独绑定可选附加服务（关联表） |
| 入口 | 服务走独立预约入口；实物商品购物车流程不变 |
| 营业时间 | 系统 24h 可访问；可选开始时间限营业时间内（复用 `system_config`） |
| 可取消 | 预约有状态（PENDING/SERVICED/CANCELLED），取消后时段释放 |
| 数据存储 | 复用 `orders`+`order_item` 存快照，新增 `appointment` 表存时间信息，`order_id` 关联 |
| 价格 | 复用现有 `PriceCalculationService`（会员按等级折扣） |
| 宠物信息 | 文本填写（不建 pet 表） |

### 冲突检测（核心算法）

```sql
-- 任意时刻同时进行数 = 与新预约重叠的非取消已有预约数 + 1（自身）
SELECT COUNT(*) FROM appointment
WHERE status <> 'CANCELLED'
  AND start_time < :newEnd     -- existing.start < newEnd
  AND end_time   > :newStart;  -- existing.end  > newStart
-- 结果 >= 3 即满，拒绝
```

### 分期实施计划

| 期 | 范围 | 产出 |
|---|---|---|
| 第 1 期 | 数据层 + 后端预约核心闭环 | DDL 加字段+2 张新表、`AppointmentService`（冲突检测+下单复用订单）、App 接口。**最小可用闭环** |
| 第 2 期 | 前端预约页 + 首页入口 | `/appointment/:productId`、`AppointmentPage.vue`、ProductCard 改"去预约" |
| 第 3 期 | 取消 / 状态流转 / 我的预约页 | 预约列表、取消按钮、状态联动 |
| 第 4 期 | Admin 配置 + 预约看板 | service_category/duration/绑定配置 UI、营业时间配置、当日预约时间线可视化 |

### 待改造的关键点（详见方案文档）

- `OrderServiceImpl.createOrder`（`OrderServiceImpl.java:52-208`）返回结果需带出 `orderId`，供 `appointment.order_id` 关联（第 1 期唯一侵入性改动）。
- `system_config.order_start/end_time` 从"预留接单时段"语义复用为"可预约开始时间范围"，需更新注释。
- `ProductCard.vue:22-42` 对 `MAIN_SERVICE` 改"去预约"按钮跳转预约页。

## 本地启动

```bash
# MySQL
cd backend && docker-compose up -d

# 后端（端口 8080，首次启动自动创建 admin/admin123）
cd backend && mvn spring-boot:run

# H5（端口 3000）
cd frontend/h5 && pnpm dev

# Admin（端口 3001）
cd frontend/admin && pnpm dev
```

## 权威文档

`modules.md` > `api.md` > `backend-implementation-notes.md` > `联调指南.md`

# PetShopOrder 后端开发规划

## Context

项目前端 H5（7 页面）和 Admin（6 页面）已基本完成，使用 MSW Mock 数据层运行。后端完全未实现。需要基于 `modules.md`（业务规范）和 `api.md`（接口规范）开发 Spring Boot 后端，使前端切换到真实 API。

**权威文档**：`modules.md` > `api.md` > `backend-implementation-notes.md`（plan.md 中部分设计已过时，以 modules.md 为准）

**API 规模**：C 端 13 个接口 + Admin 38 个接口 + 1 个 WebSocket，共约 52 个端点

---

## 技术栈与版本矩阵

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | Spring Boot 3.x 最低要求 |
| Spring Boot | 3.3.6 | 稳定版，生态成熟 |
| mybatis-spring-boot-starter | 3.0.4 | Spring Boot 3.x 专用 |
| MyBatis | 3.5.16 | 由 starter 管理 |
| pagehelper-spring-boot-starter | 2.1.0 | MyBatis 分页插件（替代 MyBatis-Plus 分页） |
| Sa-Token | 1.39.0 | sa-token-spring-boot3-starter（Boot 3 专用） |
| MySQL Connector | 8.3.0 | mysql-connector-j（由 Spring Boot 管理） |
| MySQL Server | 8.0 | Docker 容器运行 |
| HuTool | 5.8.32 | hutool-all |
| Lombok | 1.18.34 | 由 Spring Boot 管理 |

**MyBatis 注意事项**（不使用 MyBatis-Plus）：
- 需手写 Mapper XML 和 Entity 映射
- 分页使用 PageHelper（`PageHelper.startPage()`）
- 自动填充 create_time/update_time 使用 MyBatis Interceptor
- 乐观锁在 SQL 中手写 `WHERE version = #{version}`
- 通用 CRUD 需在每个 Mapper 中手写，无 BaseMapper

---

## 开发阶段

### Phase 0：项目骨架与基础设施

**目标**：搭建可运行的 Spring Boot 项目框架，Docker 运行 MySQL，连通数据库

**步骤**：
1. 使用 Spring Initializr 创建项目（Spring Boot 3.3.6, JDK 17, Maven）
2. 添加依赖：
   - `spring-boot-starter-web`
   - `spring-boot-starter-validation`
   - `mybatis-spring-boot-starter:3.0.4`
   - `pagehelper-spring-boot-starter:2.1.0`
   - `sa-token-spring-boot3-starter:1.39.0`
   - `mysql-connector-j`（Spring Boot 管理）
   - `hutool-all:5.8.32`
   - `lombok`
3. 编写 `docker-compose.yml` 中的 MySQL 服务：
   ```yaml
   mysql:
     image: mysql:8.0
     ports: 3306:3306
     environment:
       MYSQL_ROOT_PASSWORD: xxx
       MYSQL_DATABASE: petshop_order
     volumes:
       - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
       - mysql_data:/var/lib/mysql
   ```
4. 创建建表 SQL 文件 `sql/init.sql`（modules.md 中 14 张表）
5. 配置 `application.yml`：
   - 数据库连接（localhost:3306/petshop_order）
   - 服务端口 8080
   - MyBatis 配置（mapper-locations、type-aliases-package、驼峰映射）
   - PageHelper 配置（helperDialect=mysql, reasonable=true）
   - Sa-Token Cookie 配置
   - 文件上传限制（10MB）
   - 静态资源映射（上传目录）
6. 统一响应封装：`R<T>` 类（code/message/data）、分页响应 `PageResult<T>`
7. 全局异常处理：`@RestControllerAdvice`（业务异常、参数校验异常、权限异常）
8. MyBatis Interceptor：自动填充 create_time/update_time
9. Sa-Token 配置：Cookie 模式、路由拦截器
10. CORS 配置（允许 localhost:3000/3001）
11. 初始化数据：BOSS 账号（admin/admin123）
12. 统一基础 Entity：`BaseEntity`（id, createTime, updateTime）

**关键文件**：
- 建表 SQL 参考 `modules.md` 数据库设计部分
- 响应格式参考 `api.md` 通用约定

**产出**：项目可启动，Docker MySQL 运行，基础框架就绪

---

### Phase 1：认证模块（Auth）

**目标**：实现双端登录体系（C 端短信 + Admin 账号密码）

**步骤**：
1. **C 端认证**（4 个接口）
   - `POST /api/app/auth/sms-code`：发送验证码（开发期固定验证码/日志输出）
   - `POST /api/app/auth/login`：手机号 + 验证码登录，首次自动创建 `app_user`
   - `POST /api/app/auth/logout`：退出
   - `GET /api/app/auth/check`：检查登录状态 `{ loggedIn, phone }`
   - 要点：手机号唯一约束、HttpOnly Cookie、登录后检查会员身份

2. **Admin 认证**（3 个接口）
   - `POST /api/admin/auth/login`：用户名 + 密码登录
   - `POST /api/admin/auth/logout`：退出
   - `GET /api/admin/auth/profile`：获取当前管理员信息（含 role、roleLabel）
   - 要点：连续失败锁定、BOSS/MANAGER/STAFF 角色标识

3. **权限拦截器**
   - Sa-Token 路由拦截：`/api/app/**` 检查 C 端登录、`/api/admin/**` 检查 Admin 登录
   - 角色注解：`@SaCheckRole("BOSS")` 等

4. **Entity + Mapper**：
   - `AppUser` / `AppUserMapper.xml`
   - `AdminUser` / `AdminUserMapper.xml`

**验证**：Postman 测试 7 个 Auth 接口，确认 Cookie 正确设置/清除

---

### Phase 2：文件上传模块（File）

**目标**：实现图片上传和删除，供商品/分类模块使用

**步骤**：
1. `POST /api/admin/files/upload`：Multipart 上传
   - 校验：文件类型（jpg/png/webp）、大小限制
   - 存储：本地目录，按日期分子目录
   - 返回：`{ url, key }`
2. `DELETE /api/admin/files/:key`：删除文件
3. 配置 `WebMvcConfigurer.addResourceHandlers()` 映射上传目录
4. 权限：BOSS/MANAGER

**验证**：上传图片 → 确认 URL 可访问 → 删除确认

---

### Phase 3：商品与分类模块（Product & Category）

**目标**：实现分类 CRUD 和商品（含 SKU）CRUD

**步骤**：
1. **Entity + Mapper**：
   - `Category` / `CategoryMapper.xml`
   - `Product` / `ProductMapper.xml`
   - `Sku` / `SkuMapper.xml`

2. **分类管理**（4 个接口）
   - `GET /api/admin/categories`：分类列表（`?type` 过滤，含 productCount 计算子查询）
   - `POST /api/admin/categories`：创建
   - `PUT /api/admin/categories/:id`：更新
   - `DELETE /api/admin/categories/:id`：删除（检查关联商品）

3. **商品管理**（6 个接口）
   - `GET /api/admin/products`：商品列表（PageHelper 分页 + keyword/categoryId/type/status 过滤）
   - `GET /api/admin/products/:id`：商品详情（关联查 SKU 列表）
   - `POST /api/admin/products`：创建（事务写入 product + sku 列表）
   - `PUT /api/admin/products/:id`：更新（事务：先删旧 SKU 再插入新 SKU）
   - `PUT /api/admin/products/:id/status`：上下架切换
   - `DELETE /api/admin/products/:id`：删除（检查关联订单）
   - 业务规则：type 必须匹配分类 type；SERVICE 强制 `support_delivery=false`

4. **C 端商品查询**（4 个接口）
   - `GET /api/app/categories`：分类列表（仅上架商品数 > 0）
   - `GET /api/app/products`：商品列表（仅 ON_SALE，含计算字段 price/dealPrice/hasSpec）
   - `GET /api/app/categories/:categoryId/products`：按分类查商品
   - `GET /api/app/products/:id`：商品详情（含会员 dealPrice）

**关键 SQL**：
- 商品列表 `minPrice`：`SELECT MIN(price) FROM sku WHERE product_id = ?`
- C 端 `dealPrice`：需关联会员身份判断用 `price` 还是 `member_price`
- 分页：`PageHelper.startPage(page, size)` + `PageInfo<T>`

**验证**：Admin CRUD 全流程 + C 端查询过滤逻辑

---

### Phase 4：会员模块（Member）

**目标**：实现会员等级管理和会员信息管理

**步骤**：
1. **Entity + Mapper**：
   - `MemberLevel` / `MemberLevelMapper.xml`
   - `Member` / `MemberMapper.xml`
   - `MemberPhone` / `MemberPhoneMapper.xml`

2. **会员等级**（5 个接口）
   - `GET /api/admin/member-levels`：等级列表（含 memberCount 子查询）
   - `POST /api/admin/member-levels`：创建
   - `PUT /api/admin/member-levels/:id`：更新
   - `PUT /api/admin/member-levels/:id/status`：启用/禁用（检查是否有会员）
   - `DELETE /api/admin/member-levels/:id`：删除（同上）

3. **会员信息**（4 个接口）
   - `GET /api/admin/members`：会员列表（PageHelper 分页 + keyword/levelId）
   - `POST /api/admin/members`：创建（事务：member + member_phone 列表）
   - `PUT /api/admin/members/:id`：更新（事务：先删旧手机号再插入新手机号）
   - `DELETE /api/admin/members/:id`：删除

4. **C 端会员身份**
   - `GET /api/app/member/profile`：返回会员信息
   - 自动匹配：登录手机号与 `member_phone` 表关联

**验证**：创建等级 → 创建会员 → C 端登录匹配 → 查询 Profile

---

### Phase 5：价格计算引擎（Price Engine）

**目标**：实现后端权威的价格计算逻辑

**步骤**：
1. 创建 `PriceCalculationService`
2. 定价规则：
   - **GOODS**：`dealPrice = member ? sku.memberPrice : sku.price`
   - **SERVICE**：`dealPrice = member ? sku.price * discountRate : sku.price`
3. 购物车计算接口：`POST /api/app/cart/calculate`
   - 接收 items + 可选 deliveryLat/deliveryLng
   - 查询商品/SKU/会员信息
   - 返回 CalculatedItem[] + 金额汇总 + deliveryCheck

**精度规则**（必须遵守）：
- `BigDecimal` + String 构造器
- HALF_UP 四舍五入到分 **再** 乘以数量
- 金额比较用 `compareTo()`，不用 `equals()`

**验证**：会员/非会员 × GOODS/SERVICE × 单/多规格 组合测试

---

### Phase 6：配送与距离计算（Delivery）

**目标**：实现 Haversine 距离计算和配送费策略

**步骤**：
1. 创建 `DeliveryService`
2. Haversine 公式（纯本地计算，零 API 调用）
3. 配送费策略（读取系统配置）：
   - **FREE**：免配送费
   - **FIXED**：固定费用
   - **TIERED**：阶梯定价（查 `system_config_delivery_tier` 表）
4. 配送校验：距离 <= 半径、可配送商品原价 >= 起送金额、至少含一个 support_delivery 商品

**验证**：不同距离/金额组合

---

### Phase 7：系统配置模块（System Config）

**目标**：系统配置管理，为配送和订单提供配置

**步骤**：
1. **Entity + Mapper**：
   - `SystemConfig` / `SystemConfigMapper.xml`
   - `SystemConfigDeliveryTier` / `SystemConfigDeliveryTierMapper.xml`
   - `SystemConfigLog` / `SystemConfigLogMapper.xml`

2. `GET /api/admin/system-config`：获取配置（含变更日志）
   - Webhook URL 脱敏（`key=***`）
   - 返回 `hasQywxWebhook` 布尔值

3. `PUT /api/admin/system-config`：更新配置
   - Webhook 语义：缺失=不变、空串=清除、有值=更新
   - 事务写入：config + tier rules（先删旧再插新）+ log
   - Webhook key AES 加密存储

4. `POST /api/admin/system-config/test-webhook`：测试 Webhook
   - **SSRF 防护**：仅允许 `https://qyapi.weixin.qq.com`、禁止内网地址、禁止 HTTP
   - 频率限制

5. BOSS 权限限制

**验证**：配置读取/更新/审计日志/Webhook 测试

---

### Phase 8：订单模块（Order）

**目标**：订单创建和查询——核心交易环节

**步骤**：
1. **Entity + Mapper**：
   - `Orders` / `OrdersMapper.xml`
   - `OrderItem` / `OrderItemMapper.xml`

2. **创建订单**：`POST /api/app/orders`
   - 调用价格计算引擎（后端权威定价）
   - 调用配送校验（如需配送）
   - 事务写入 `orders` + `order_item`（含快照字段）
   - SKU 库存扣减：`UPDATE sku SET stock = stock - #{qty} WHERE id = #{id} AND stock >= #{qty}`
   - 订单号：日期 + 雪花算法（HuTool `IdUtil.getSnowflakeNextIdStr()`）
   - 异步触发企微 Webhook
   - 订单时间窗口校验

3. **C 端查询**
   - `GET /api/app/orders`：我的订单列表（PageHelper 分页）
   - `GET /api/app/orders/:id`：订单详情

4. **Admin 查询与管理**
   - `GET /api/admin/orders`：订单列表（分页 + 多条件过滤）
   - `GET /api/admin/orders/:id`：订单详情（含 raw phone、坐标）
   - `PUT /api/admin/orders/:id/processed`：标记已处理/未处理
   - customerPhone 脱敏（中间4位*号）

**验证**：创建 GOODS+SERVICE 订单 → 验证价格快照 → Admin 查看 → 标记处理

---

### Phase 9：通知模块（Notification）

**目标**：新订单推送通知

**步骤**：
1. **企微群机器人 Webhook**
   - `@Async` 异步发送（不阻塞下单）
   - 消息格式：订单摘要（JSON card/markdown）
   - 失败静默（不影响下单）
   - 20 条/分钟限制

2. **WebSocket 推送**（Admin 端）
   - 端点：`/api/admin/ws/orders`
   - 使用 Spring WebSocket (`spring-boot-starter-websocket`)
   - 新订单时推送 `NEW_ORDER` 消息
   - 连接验证 Admin 身份（Sa-Token + WebSocket 握手拦截）

**验证**：创建订单 → 确认企微消息 → Admin WebSocket 收到推送

---

### Phase 10：Admin 用户管理（Admin User）

**目标**：管理员账号 CRUD（BOSS 专属）

**步骤**：
1. 复用 Phase 1 的 `AdminUserMapper`，扩展查询方法
2. 6 个接口：列表/创建/更新/启禁/重置密码/删除
3. 创建仅限 MANAGER/STAFF 角色
4. 密码加密存储（BCrypt 或 Sa-Token 的 `SecureUtil.md5()`）
5. BOSS 权限限制

**验证**：创建各角色管理员 → 验证权限矩阵

---

### Phase 11：操作日志（Operation Log）

**目标**：关键操作审计日志

**步骤**：
1. **Entity + Mapper**：`OperationLog` / `OperationLogMapper.xml`
2. `GET /api/admin/operation-logs`：日志列表（PageHelper 分页，所有角色可查看）
3. `@OperationLog` 注解 + AOP 切面自动记录：
   - 操作人、操作类型、目标、变更前/后值、时间戳
4. 在关键 Service 方法上添加注解（价格变更、等级调整、配置变更）

**验证**：执行关键操作后查看日志

---

### Phase 12：数据统计（Statistics）

**目标**：Admin 端数据概览

**步骤**：
1. `GET /api/admin/stats/overview`：总览（聚合 SQL）
2. `GET /api/admin/stats/order-trends`：订单趋势（GROUP BY 日期）
3. `GET /api/admin/stats/member-ranking`：会员排名（JOIN 订单表）
4. BOSS/MANAGER 权限

**验证**：有订单数据后查询统计接口

---

### Phase 13：集成测试与前端联调

**目标**：前后端联通，关闭 MSW Mock

**步骤**：
1. 关闭前端 MSW（`VITE_USE_MOCK=false`）
2. 确认 Vite 代理指向 `http://localhost:8080`
3. 全流程测试：登录 → 浏览 → 加购 → 结算 → 下单 → Admin 查看
4. 权限矩阵验证：BOSS/MANAGER/STAFF 各角色
5. 边界场景：会员/非会员价格、配送/自提、库存并发、距离边界、时间窗口

---

### Phase 14：部署准备

**目标**：Docker Compose 单机部署

**步骤**：
1. 完善 `docker-compose.yml`（MySQL + Spring Boot + Nginx）
2. 编写 `Dockerfile`（Java 应用镜像，基于 eclipse-temurin:17-jre）
3. Nginx 配置：前端静态文件 + `/api` 反向代理 + 上传目录映射
4. 环境变量（数据库连接、文件存储路径、Webhook 等）
5. 初始化脚本（建表 SQL + BOSS 账号）

---

## 模块依赖关系

```
Phase 0 (基础设施 + Docker MySQL)
  ├── Phase 1 (认证) ──────────────────── 所有模块前置
  ├── Phase 2 (文件上传) ──────────────── 商品模块前置
  ├── Phase 3 (商品/分类) ← 依赖 Phase 1, 2
  ├── Phase 4 (会员) ← 依赖 Phase 1
  ├── Phase 7 (系统配置) ← 依赖 Phase 1
  │
  ├── Phase 5 (价格引擎) ← 依赖 Phase 3, 4
  ├── Phase 6 (配送距离) ← 依赖 Phase 7
  │
  ├── Phase 8 (订单) ← 依赖 Phase 5, 6, 7
  ├── Phase 9 (通知) ← 依赖 Phase 8
  │
  ├── Phase 10 (Admin用户) ← 依赖 Phase 1
  ├── Phase 11 (操作日志) ← 依赖 Phase 1
  ├── Phase 12 (数据统计) ← 依赖 Phase 8
  │
  ├── Phase 13 (集成联调) ← 所有模块完成后
  └── Phase 14 (部署) ← Phase 13 通过后
```

**可并行开发**：
- Phase 3（商品）和 Phase 4（会员）可并行
- Phase 10（Admin 用户）和 Phase 11（操作日志）可并行
- Phase 7（系统配置）可与 Phase 3/4 并行

---

## 每个 Phase 的 Entity + Mapper XML 清单

使用 MyBatis（非 MyBatis-Plus）需要为每张表手写 Mapper XML：

| Phase | Entity | Mapper XML | 核心查询 |
|-------|--------|-----------|---------|
| 1 | AppUser, AdminUser | AppUserMapper, AdminUserMapper | 按手机号/用户名查找 |
| 3 | Category, Product, Sku | CategoryMapper, ProductMapper, SkuMapper | 列表+分页+SKU聚合 |
| 4 | MemberLevel, Member, MemberPhone | MemberLevelMapper, MemberMapper, MemberPhoneMapper | 多手机号关联+会员匹配 |
| 7 | SystemConfig, SystemConfigDeliveryTier, SystemConfigLog | 对应 Mapper | 单行配置+阶梯规则 |
| 8 | Orders, OrderItem | OrdersMapper, OrderItemMapper | 库存扣减乐观锁+快照 |
| 11 | OperationLog | OperationLogMapper | 时间范围分页查询 |

---

## 关键参考文件

| 文件 | 用途 |
|------|------|
| `modules.md` | 权威业务规范（数据库设计、业务规则） |
| `api.md` | 权威接口规范（请求/响应格式） |
| `backend-implementation-notes.md` | 系统配置安全要求 |
| `frontend/admin/src/types/index.ts` | Admin 前端 TypeScript 类型定义 |
| `frontend/h5/src/types/index.ts` | H5 前端 TypeScript 类型定义 |
| `frontend/admin/src/api/*.ts` | Admin 前端 API 调用实现 |
| `frontend/h5/src/api/*.ts` | H5 前端 API 调用实现 |
| `frontend/admin/src/mocks/*.ts` | Admin MSW Mock（参考响应数据格式） |
| `frontend/h5/src/mocks/*.ts` | H5 MSW Mock（参考响应数据格式） |

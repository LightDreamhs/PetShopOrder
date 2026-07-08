# 服务预约系统 - 实施方案

> ✅ **状态**：已实施完成并上线（2026-07-08，后端 + H5 + Admin 三端）
> 📅 **定稿日期**：2026-06-30 ｜ **完成日期**：2026-07-08
> 🎯 **目标**：把"像商品一样直接下单"的服务，改造为"主服务 + 附加服务 + 选时间"的**预约系统**，并支持**时间段冲突检测**。
> 📎 本文档原为**实施手册**，现已全部落地。下方分阶段步骤的 checkbox 反映实际完成情况，文末「八、实施差异」记录与原方案的偏差。

---

## 〇、需求边界确认（全部经逐条确认）

| 维度 | 决策 | 备注 |
|---|---|---|
| **核心约束** | 任意时刻同时进行的服务 **≤ 3 个** | 全局容量 = 3，留余量 |
| **资源模型** | 不区分美容师 / 工位 | 只数"全局同时进行的服务数"，不按人/台调度 |
| **冲突算法** | 区间重叠计数 | 新预约插入后，重叠数 > 3 则拒绝 |
| **并发安全** | 应用层检查即可 | 单店低流量；极端竞态事后人工处理（**不加 DB 锁**） |
| **服务时长** | 按 SKU 配 `duration`（分钟） | 店主自填；猫狗、洗护/美容各不同 |
| **总占用时长** | 主服务 duration + Σ 附加服务 duration | 累加后用于冲突检测 |
| **时间粒度** | 精确任意时间（如 09:30） | 用户自选开始时间，不做固定 slot |
| **下单模式** | **1 主服务 + N 附加服务** | 主服务占时间段，附加服务跟着主服务走 |
| **主/附加区分** | `product.service_category` | `MAIN_SERVICE` / `ADDON_SERVICE` |
| **附加服务范围** | 每个主服务**单独绑定**可选附加服务 | 用关联表 |
| **入口** | 服务走**独立预约入口** | 实物商品（GOODS）购物车流程**完全不变** |
| **营业时间** | 系统 24h 可访问；可选开始时间限定营业时间内 | 复用 `system_config` 的 start/end_time |
| **可取消** | 预约有状态 | PENDING / SERVICED / CANCELLED，取消后时段释放 |
| **数据存储** | 复用 `orders` + `order_item` 存快照 | 新增 `appointment` 表存时间信息，`order_id` 关联 |
| **明细存储** | 主服务 + 每个附加服务**各一条** order_item | 复用现有快照机制 |
| **价格** | 复用现有 `PriceCalculationService` | 主服务 SKU 价 + 附加服务 SKU 价，会员按等级折扣 |
| **宠物信息** | **文本填写**，不建 pet 表 | 存入 appointment.pet_info |

---

## 一、整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│  H5 首页 HomePage.vue                                            │
│  ├── GOODS 商品 → ProductCard（加减/选规格）→ 购物车（不变）      │
│  └── MAIN_SERVICE 服务 → ProductCard【去预约】按钮               │
│                                   │                              │
│                                   ▼                              │
│                    /appointment/:productId                       │
│                    AppointmentPage.vue（新增）                   │
│                    ┌──────────────────────────────┐             │
│                    │ 1. 宠物信息（文本）           │             │
│                    │ 2. 主服务 SKU（带 duration）  │             │
│                    │ 3. 附加服务多选（按绑定）     │             │
│                    │ 4. 选择开始时间（限营业时间） │             │
│                    │ 5. 实时算价 + 冲突预检        │             │
│                    │ 6. 提交 → 生成订单 + 预约     │             │
│                    └──────────────────────────────┘             │
└─────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│  后端 AppAppointmentController                                   │
│  ├── GET  /addons?mainProductId=  → 该主服务绑定的附加服务       │
│  ├── POST /check                  → 冲突预检（前端选时间时实时） │
│  ├── POST /create                 → 下单+预约（同事务）          │
│  ├── POST /{id}/cancel            → 取消（释放时段）             │
│  └── GET  /mine                   → 我的预约列表                 │
└─────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│  AppointmentService                                              │
│  ├── checkConflict(start, totalDuration)  ← 核心：重叠计数       │
│  ├── createAppointment(...)  ← 复用 OrderService.createOrder     │
│  │                              后再 insert appointment          │
│  ├── cancel(id)              ← status → CANCELLED               │
│  └── myAppointments(userId)                                    │
└─────────────────────────────────────────────────────────────────┘
```

**关键设计取舍**：
- 预约**复用**现有订单体系（`orders` + `order_item` + 算价 + 通知），只在订单上"挂载"一条 `appointment` 记录。**不重写支付/通知/会员逻辑**。
- 冲突检测是**纯应用层**的区间重叠计数 SQL，不引入复杂调度引擎。

---

## 二、数据层设计

### 2.1 改动现有表

#### `product` 表：新增服务子类字段
```sql
ALTER TABLE product
  ADD COLUMN service_category VARCHAR(16) NULL
  COMMENT '服务子类：MAIN_SERVICE 主服务 / ADDON_SERVICE 附加服务。仅 type=SERVICE 时有效，GOODS 为 NULL'
  AFTER type;
ALTER TABLE product
  ADD INDEX idx_product_service_category (type, service_category, status);
```
- 同步给现有服务数据初始化（执行前手工指定，或在 Admin 配好后批量 UPDATE）。

#### `sku` 表：新增服务时长字段
```sql
ALTER TABLE sku
  ADD COLUMN duration INT NULL COMMENT '服务时长（分钟）。仅 SERVICE 的 SKU 用，GOODS 为 NULL'
  AFTER price;
```
- 初始化示例（按 SKU 自填）：`UPDATE sku SET duration=110 WHERE product_id=11 AND spec_name='全套精修';`

### 2.2 新增表

#### `main_service_addon` —— 主服务与附加服务的绑定关系
```sql
CREATE TABLE IF NOT EXISTS main_service_addon (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    main_product_id    BIGINT      NOT NULL COMMENT '主服务 product.id（service_category=MAIN_SERVICE）',
    addon_product_id   BIGINT      NOT NULL COMMENT '附加服务 product.id（service_category=ADDON_SERVICE）',
    sort               INT         NOT NULL DEFAULT 0,
    create_time        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_main_addon (main_product_id, addon_product_id),
    INDEX idx_addon_main (addon_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主服务-附加服务绑定';
```

#### `appointment` —— 预约记录（核心）
```sql
CREATE TABLE IF NOT EXISTS appointment (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT       NOT NULL COMMENT '关联 orders.id（一笔预约对应一个订单）',
    user_id         BIGINT       NOT NULL COMMENT 'app_user.id',
    main_product_id BIGINT       NOT NULL COMMENT '主服务 product.id 快照',
    main_sku_id     BIGINT       NOT NULL COMMENT '主服务 sku.id 快照',
    start_time      DATETIME     NOT NULL COMMENT '预约开始时间（顾客到店/服务开始）',
    end_time        DATETIME     NOT NULL COMMENT 'start_time + total_duration',
    total_duration  INT          NOT NULL COMMENT '总占用时长（分钟）= 主 + Σ附加',
    pet_info        VARCHAR(255) NULL COMMENT '宠物信息（文本：名字/种类/体重/性格等）',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING 待服务 / SERVICED 已完成 / CANCELLED 已取消',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_appointment_order (order_id),
    INDEX idx_appointment_user (user_id),
    INDEX idx_appointment_status_time (status, start_time, end_time),
    INDEX idx_appointment_time_range (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务预约';
```

**字段说明**：
- `start_time` / `end_time`：预约时间区间，`end_time = start_time + total_duration 分钟`。
- `total_duration`：冗余存储（避免每次重算），= 主服务 SKU duration + Σ 附加服务 SKU duration。
- `status`：取消时置 `CANCELLED`，**冲突检测必须排除 CANCELLED**。
- `idx_appointment_time_range`：冲突查询的核心索引（按时间范围扫描）。

### 2.3 复用现有配置

`system_config` 表已有的营业时间字段（`init.sql:181-183`），从"预留"转为正式启用：
- `order_time_enabled` TINYINT —— 是否启用营业时间限制
- `order_start_time` TIME —— 营业开始（如 09:00）
- `order_end_time` TIME —— 营业结束（如 21:00）

> ⚠️ 注意：原注释写的是"接单时段开关"（控制能否下单），这里**复用为"可预约时段"**。语义略有差异但字段通用，需在文档/注释中明确：此字段现表示**预约可选开始时间的范围**。

### 2.4 实体类 / Mapper 清单（新增）

| 类型 | 文件路径（`backend/src/main/java/com/petshop/order/`） |
|---|---|
| 实体 | `entity/MainServiceAddon.java`、`entity/Appointment.java` |
| Mapper | `mapper/MainServiceAddonMapper.java` + `mapper/xml/MainServiceAddonMapper.xml`、`mapper/AppointmentMapper.java` + `mapper/xml/AppointmentMapper.xml` |
| 改动实体 | `entity/Product.java` 加 `serviceCategory`、`entity/Sku.java` 加 `duration` |

---

## 三、冲突检测算法（核心）

### 3.1 问题定义

> 任意时刻同时进行的服务不超过 3 个。给定新预约 `[start, end)`，判断它与已有预约的重叠数是否会让某时刻超过 3。

### 3.2 区间重叠判定（经典）

两个区间 `[s1, e1)` 与 `[s2, e2)` **重叠** ⟺ `s1 < e2 AND s2 < e1`。

### 3.3 容量校验

新预约若可插入，则新预约所在时间段内"同时进行的服务数（含自身）"≤ 3。
关键洞察：**新预约区间内任意时刻的重叠数 = 与新预约重叠的已有（非取消）预约数 + 1**（自身）。
> 因为新预约在自己的整个区间内始终存在，所以只需数"与我重叠的已有预约数"，≥3 即满。

### 3.4 SQL 实现（应用层，不加锁）

```sql
-- 冲突检测：数与新预约重叠的、非取消的、已有预约数
SELECT COUNT(*) AS overlap_count
FROM appointment
WHERE status <> 'CANCELLED'
  AND start_time < :newEnd     -- existing.start < newEnd
  AND end_time   > :newStart;  -- existing.end  > newStart
```

- 结果 `overlap_count >= 3` → **该时段已约满**，拒绝。
- `overlap_count <= 2` → 可预约（加上自身 ≤3）。

### 3.5 Java 伪代码

```java
/**
 * 冲突检测：新预约 [start, start+totalDurationMin) 插入后是否超过全局容量 3。
 * @return true 表示可预约，false 表示该时段已约满
 */
public boolean isSlotAvailable(LocalDateTime start, int totalDurationMin) {
    LocalDateTime end = start.plusMinutes(totalDurationMin);
    int overlap = appointmentMapper.countOverlap(start, end);  // 上面的 SQL
    return overlap < MAX_CONCURRENT;  // MAX_CONCURRENT = 3
}
```

```java
// AppointmentServiceImpl.createAppointment 内（@Transactional）
if (!isSlotAvailable(startTime, totalDuration)) {
    throw new BusinessException("该时段已约满，请选择其他时间");
}
// ... 生成 orders + order_item（复用 OrderService）...
// insert appointment（status=PENDING）
```

### 3.6 营业时间校验（在冲突检测之前）

```java
// 读取 system_config（缓存）
SystemConfig cfg = ...;
if (cfg.getOrderTimeEnabled() == 1) {
    LocalTime t = startTime.toLocalTime();
    if (t.isBefore(cfg.getOrderStartTime()) || !t.isBefore(cfg.getOrderEndTime())) {
        throw new BusinessException("请在营业时间内预约（" + cfg.getOrderStartTime() + "-" + cfg.getOrderEndTime() + "）");
    }
}
```
> 注意 `end_time`（营业结束）边界：用 `!t.isBefore(end)` 即 `t >= end` 拒绝，保证可约到 `end` 前一秒。

### 3.7 并发说明（已确认）

- **应用层检查即可**，不加数据库行锁/唯一约束。
- 极端竞态（两人同时抢最后一个名额，都通过检查）概率极低，事后由店主人工协调。
- 若未来流量增大需加固：可在 `appointment` 表加 `SELECT ... FOR UPDATE` 包住检查+插入，或用唯一索引兜底。**本期不做**。

---

## 四、后端实现清单

### 4.1 新增 Service

`service/AppointmentService.java` + `service/impl/AppointmentServiceImpl.java`：

| 方法 | 职责 |
|---|---|
| `getAddons(mainProductId)` | 查 `main_service_addon` JOIN `product`，返回该主服务绑定的附加服务列表（含 SKU/duration/price） |
| `isSlotAvailable(start, totalDuration)` | 见 [3.5](#35-java-伪代码)，冲突检测 |
| `createAppointment(req)` | 见下，**@Transactional**：算总时长→营业时间校验→冲突校验→复用下单→插预约 |
| `previewConflict(start, mainSkuId, addonSkuIds)` | 前端选时间时实时调用，返回 `{available, overlapCount, message}` |
| `cancel(appointmentId, userId)` | 校验归属→status=CANCELLED（释放时段）→订单标记 |
| `myAppointments(userId, page, size)` | 分页查我的预约，含订单快照信息 |

#### `createAppointment` 核心流程（关键代码路径）

```java
@Transactional(rollbackFor = Exception.class)
public Map<String, Object> createAppointment(Map<String, Object> req) {
    // 1. 解析：mainProductId, mainSkuId, addonSkuIds[], startTime, petInfo, customerName

    // 2. 算总时长：主 SKU.duration + Σ 附加 SKU.duration
    int mainDuration = skuMapper.selectById(mainSkuId).getDuration();
    int addonDuration = addonSkuIds.stream()
        .mapToInt(id -> skuMapper.selectById(id).getDuration()).sum();
    int totalDuration = mainDuration + addonDuration;

    // 3. 营业时间校验（见 3.6）

    // 4. 冲突校验（见 3.5）
    if (!isSlotAvailable(startTime, totalDuration)) {
        throw new BusinessException("该时段已约满，请选择其他时间");
    }

    // 5. 复用现有 OrderService.createOrder 生成 orders + order_item
    //    构造 items = [mainSku] + addonSkus，quantity=1，needDelivery=false（服务到店）
    Map<String,Object> orderReq = buildOrderReq(...);  // needDelivery=false
    Map<String,Object> orderResult = orderService.createOrder(orderReq);
    Long orderId = ...;  // 从 orderResult 或内部返回获取

    // 6. 插入 appointment 记录
    Appointment appt = new Appointment();
    appt.setOrderId(orderId);
    appt.setUserId(currentUser.getId());
    appt.setMainProductId(mainProductId);
    appt.setMainSkuId(mainSkuId);
    appt.setStartTime(startTime);
    appt.setEndTime(startTime.plusMinutes(totalDuration));
    appt.setTotalDuration(totalDuration);
    appt.setPetInfo(petInfo);
    appt.setStatus("PENDING");
    appointmentMapper.insert(appt);

    return result;  // 含 orderNo, appointmentId, startTime, totalAmount
}
```

> 💡 **复用要点**：`OrderServiceImpl.createOrder`（`OrderServiceImpl.java:52-208`）已处理算价、会员、订单号、明细、通知。预约下单只需把主服务 + 附加服务作为 `items` 传入，`needDelivery=false`（服务强制到店，现有逻辑 `ProductServiceImpl` 已保证 SERVICE 的 `supportDelivery=0`）。
>
> ⚠️ **改造点**：现有 `OrderServiceImpl.createOrder` 内部通过 `req` 的 `items` 列表驱动，返回结果里**没有 orderId**（只返回 orderNo）。需要让它在结果 Map 中带出 `order.getId()`，或在预约 Service 内部直接组装 Orders/OrderItem（复制其逻辑）。**推荐**：让 `createOrder` 结果带回 `id`，最小侵入。

### 4.2 新增 Controller

`controller/app/AppAppointmentController.java`：

```java
@RestController
@RequestMapping("/api/app/appointments")
@RequiredArgsConstructor
public class AppAppointmentController {
    private final AppointmentService appointmentService;
    private final AppAuthService appAuthService;

    // 主服务的附加服务列表
    @GetMapping("/addons")
    public R<List<Map<String,Object>>> addons(@RequestParam Long mainProductId) { ... }

    // 冲突预检（选时间时实时调用）
    @PostMapping("/check")
    public R<Map<String,Object>> check(@RequestBody Map<String,Object> req) { ... }

    // 创建预约（同事务生成订单+预约）
    @PostMapping
    public R<Map<String,Object>> create(@RequestBody Map<String,Object> req) {
        AppUser u = appAuthService.getCurrentUser();
        req.put("_userId", u.getId());
        return R.ok(appointmentService.createAppointment(req));
    }

    // 取消
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) { ... }

    // 我的预约
    @GetMapping("/mine")
    public R<PageResult<Map<String,Object>>> mine(@RequestParam(defaultValue="1") int page,
                                                  @RequestParam(defaultValue="10") int size) { ... }
}
```

### 4.3 Admin 配置接口（第4期）

`controller/admin/AdminAppointmentController.java` 或扩展现有 Admin 商品接口：

| 能力 | 接口 |
|---|---|
| 设置商品 `service_category` | 扩展商品新增/编辑接口 |
| 设置 SKU `duration` | 扩展 SKU 新增/编辑接口 |
| 管理主服务-附加服务绑定 | `POST/DELETE /api/admin/products/{mainId}/addons/{addonId}` |
| 配置营业时间 | 扩展现有 `system_config` 编辑接口（已存在） |
| 预约看板 | `GET /api/admin/appointments?date=`（第4期） |

### 4.4 新增 API（写入 `api.md`）

需在 `api.md` 补充上述 App / Admin 预约相关接口文档。

---

## 五、分阶段实施步骤

> 建议按顺序推进，每期独立可测、可上线。**第 1+2 期即可形成最小可用闭环**。

### 第 1 期：数据层 + 后端预约核心闭环（后端）
- [x] 执行 [2.1](#21-改动现有表) / [2.2](#22-新增表) DDL（product、sku 加字段 + 2 张新表）
- [x] 新增/改动实体类（`Product.serviceCategory`、`Sku.duration`、`MainServiceAddon`、`Appointment`）
- [x] 新增 Mapper + XML（含 `AppointmentMapper.countOverlap` 冲突查询）
- [x] 实现 `AppointmentService`（getAddons / isSlotAvailable / createAppointment / previewConflict / cancel / myAppointments）
- [x] 改造 `OrderServiceImpl.createOrder`：结果 Map 带出 `orderId`
- [x] 新增 `AppAppointmentController`
- [x] 补充 `init.sql`：给现有服务数据初始化 `service_category` + `duration` + 一组绑定示例
- [x] Postman / 联调验证：建预约→冲突→取消→释放

**验收**：能通过接口完成"选主服务+附加+时间→下单→生成订单+预约"，同时间段第 4 次被拒。

### 第 2 期：前端预约页 + 首页入口（H5）
- [x] 新增类型定义（`types/index.ts`：Appointment、AppointmentCreateReq 等）
- [x] 新增 API 封装（`api/appointment.ts`）
- [x] 新增路由 `/appointment/:productId`（`router/index.ts`）
- [x] 新增 `views/AppointmentPage.vue`：宠物信息→主服务SKU(带duration)→附加服务多选(按绑定)→时间选择(限营业时间)→实时算价+冲突预检→提交
- [x] 改造 `components/home/ProductCard.vue`：`type==='SERVICE' && serviceCategory==='MAIN_SERVICE'` 时按钮显示"去预约"，点击 `router.push('/appointment/'+id)`
- [x] 改造 `views/HomePage.vue`：处理新的"去预约"事件（替代/并存于现有 selectSpec）
- [x] 订单详情 `OrderDetailPage.vue` / 下单成功 `OrderSuccessPage.vue`：展示预约时间、宠物信息

**验收**：首页点服务→进预约页→选时间下单→订单详情看到预约时间；冲突时实时提示。

### 第 3 期：取消 / 状态流转 / 我的预约页（H5 + 后端）
- [x] 新增 `views/AppointmentListPage.vue`（我的预约，按状态筛选）
- [x] 预约详情：取消按钮（状态 PENDING 可取消）
- [x] 后端 `cancel` 联动订单状态/通知
- [x] 状态流转：PENDING → SERVICED（店主手动标记完成，第4期 Admin 做）

**验收**：用户可查看/取消自己的预约；取消后该时段重新可约。

### 第 4 期：Admin 配置 + 预约看板（后台）
- [x] 商品管理：service_category 配置 UI
- [x] SKU 管理：duration 配置 UI
- [x] 主服务-附加服务绑定管理 UI
- [x] 营业时间配置（复用现有 system_config 编辑）
- [x] 预约看板：按日期展示当日预约时间线（可视化重叠/容量）

**验收**：店主无需改库即可完成全部配置。

---

## 六、涉及的关键文件路径（file_path:line）

### 后端
| 文件 | 行 | 说明 |
|---|---|---|
| `backend/src/main/java/com/petshop/order/service/impl/OrderServiceImpl.java` | 52-208 | `createOrder`，需改造带出 orderId |
| `backend/src/main/java/com/petshop/order/service/impl/PriceCalculationServiceImpl.java` | 35-108 | 算价（直接复用，不改） |
| `backend/src/main/java/com/petshop/order/entity/Product.java` | 12-24 | 加 `serviceCategory` |
| `backend/src/main/java/com/petshop/order/entity/Sku.java` | 11-19 | 加 `duration` |
| `backend/src/main/java/com/petshop/order/entity/Orders.java` | 11-30 | 订单主表（复用） |
| `backend/src/main/java/com/petshop/order/entity/OrderItem.java` | 11-23 | 明细（复用） |
| `backend/src/main/java/com/petshop/order/controller/app/AppOrderController.java` | 16-41 | 参考风格 |
| `backend/sql/init.sql` | 14-26 | product 表定义 |
| `backend/sql/init.sql` | 29-40 | sku 表定义 |
| `backend/sql/init.sql` | 95-139 | orders / order_item |
| `backend/sql/init.sql` | 181-183 | system_config 营业时间（复用） |

### 前端（H5）
| 文件 | 行 | 说明 |
|---|---|---|
| `frontend/h5/src/views/HomePage.vue` | 64-84, 174-204 | 商品列表、加购/选规格入口 |
| `frontend/h5/src/components/home/ProductCard.vue` | 22-42 | 按钮区，改"去预约" |
| `frontend/h5/src/router/index.ts` | 5-54 | 路由表，加预约路由 |
| `frontend/h5/src/types/index.ts` | 59-70, 114-177 | CartItem / 订单类型，加预约类型 |
| `frontend/h5/src/views/CheckoutPage.vue` | 11 | 备注 placeholder（服务预约改走独立页后可还原） |
| `frontend/h5/src/views/OrderDetailPage.vue` | — | 加预约时间展示 |
| `frontend/h5/src/views/OrderSuccessPage.vue` | — | 加预约时间展示 |

---

## 七、风险与备忘

1. **`OrderServiceImpl.createOrder` 改造风险**：当前它返回的结果不含 `orderId`，预约 Service 需要它来关联 appointment。最小侵入做法是让它在返回 Map 中加 `result.put("id", order.getId())`。
2. **营业时间字段语义**：`system_config.order_start/end_time` 原注释是"接单时段"，现复用为"可预约开始时间范围"，需更新注释，避免后人误解。
3. **附加服务时长累加**：冲突检测用的是**总时长（主+附加）**，UI 上要向用户展示总占用时长，避免误解"主服务才 110 分钟为何占了 125"。
4. **并发安全**：本期不加 DB 锁，单店场景可接受。若日后连锁化/高并发，需在 `appointment` 表用 `SELECT ... FOR UPDATE` 或唯一约束兜底。
5. **时区**：`start_time` 用 `DATETIME`，确保服务器/MySQL 时区一致（现有订单已用 DATETIME，沿用即可）。
6. **历史数据**：现有 SERVICE 商品的 `service_category` 为 NULL，需在第 1 期 DDL 后批量初始化（区分哪些是主、哪些是附加）。
7. **宠物信息**：本期纯文本，若未来要做"宠物档案/历史记录"，可再抽 `pet` 表，appointment 加 `pet_id`。

---

*本文档原为执行手册，现已全部落地，保留作为设计参考。*

---

## 八、实施差异（2026-07-08 上线后回填）

实际落地与原方案的偏差：

| 项 | 方案 | 实际 | 原因 |
|---|---|---|---|
| 时段选择 | 精确任意时间（如 09:33） | 半小时步进网格（`GET /slots`） | 方便用户选点，UI 用时间网格铺开展示 |
| 附加服务时长 | 全部累加进 `total_duration` | `duration` 缺省 0 表示只加钱不占时间 | 部分附加项（如加香波）不应占用时间段 |
| Admin 标记完成 / 取消 | 第 4 期做 | 已实现（`PUT /{id}/serviced`、`PUT /{id}/cancel`），看板 `BookingBoardPage.vue` | — |
| 订单取消联动 | 未在方案明列 | 取消预约联动 `orders.cancelled = 1` | 商家后台据此识别已取消的预约单 |
| 预约通知 | 复用订单通知 | 复用 `NotificationService`，通知体带预约时间、总时长、宠物信息 | 新增 `AppointmentNotifyInfo` |
| 迁移脚本 | 直接改 `init.sql` | `init.sql` 已含全部结构，另保留 `migration_appointment_v1.sql` 供旧库增量 | 双轨：全新部署 vs 已有库 |

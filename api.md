# 宠物店下单系统 API 接口文档

> 基于 modules.md 生成。统一服务，URL 前缀区分：`/api/app/*`（C 端）、`/api/admin/*`（管理端）。

---

## 通用约定

### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": T
}
```

### 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [T],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

### 错误码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 / 业务校验失败 |
| 401 | 未登录或登录态过期 |
| 403 | 无权限（角色不足） |
| 500 | 服务器内部错误 |

### 认证

采用 Cookie 会话方案（可用 Sa-Token 或同类实现），登录态通过 HttpOnly cookie 自动携带，前端无需手动设置 Header。

### 分页参数

所有分页接口统一使用 Query 参数：`page`（默认 1）、`size`（默认 20，最大 100，超过自动截断）。

### 分页边界

`page < 1` 返回第一页，`page > 总页数` 返回空列表。

### 金额字段

所有金额字段类型为 `String`（如 `"84.15"`），精度 2 位小数。前端展示时直接使用。

---

## 一、C 端接口（/api/app）

---

### 1. 认证

#### POST /api/app/auth/sms-code

发送短信验证码。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | String | 是 | 手机号，11 位 |

**响应 data**：`null`

**业务规则**：
- 同一手机号 60 秒内不可重发
- 每日同一手机号最多发送 10 次

---

#### POST /api/app/auth/login

手机号 + 验证码登录。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | String | 是 | 手机号 |
| code | String | 是 | 4 位验证码 |

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| phone | String | 脱敏手机号，如 `138****8888` |
| isNew | Boolean | 是否新注册用户 |

**业务规则**：
- 首次登录自动创建 `app_user` 记录
- 登录成功后 Sa-Token 写入 cookie，自动续期

---

#### POST /api/app/auth/logout

退出登录。

**请求体**：无

**响应 data**：`null`

---

#### GET /api/app/auth/check

检查当前登录态是否有效。

**请求体**：无

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| loggedIn | Boolean | 是否已登录 |
| phone | String | 脱敏手机号（未登录时为 null） |

---

### 2. 商品浏览

#### GET /api/app/categories

获取商品分类列表。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 否 | 筛选类型：`GOODS` / `SERVICE`，不传则返回全部 |

**响应 data**：`List<Category>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 分类 ID |
| name | String | 分类名称 |
| icon | String | 分类图标 URL |
| type | String | `GOODS` / `SERVICE` |
| sort | Integer | 排序值（升序） |

---

#### GET /api/app/products

按类型获取在售商品列表（C 端首页主接口）。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 否 | `GOODS` / `SERVICE`，不传则返回全部 |

**响应 data**：`List<Product>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 商品 ID |
| name | String | 商品名称 |
| coverImg | String | 封面图 URL |
| type | String | `GOODS` / `SERVICE` |
| supportDelivery | Boolean | 是否支持配送（仅 GOODS 有意义） |
| price | String | 最低 SKU 原价（如 `"79.00"`） |
| dealPrice | String | 当前用户的最低 SKU 成交价（已根据会员等级计算） |
| hasSpec | Boolean | 是否有多规格 |

---

#### GET /api/app/categories/{categoryId}/products

获取某分类下的在售商品列表。

**Path 参数**：`categoryId`

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键词（匹配商品名称） |

**响应 data**：`List<Product>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 商品 ID |
| name | String | 商品名称 |
| coverImg | String | 封面图 URL |
| type | String | `GOODS` / `SERVICE` |
| supportDelivery | Boolean | 是否支持配送（仅 GOODS 有意义） |
| price | String | 最低 SKU 原价（如 `"79.00"`） |
| dealPrice | String | 当前用户的最低 SKU 成交价（已根据会员等级计算） |
| hasSpec | Boolean | 是否有多规格 |

---

#### GET /api/app/products/{id}

获取商品详情（含 SKU 列表）。

**Path 参数**：`id`（商品 ID）

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 商品 ID |
| name | String | 商品名称 |
| description | String | 商品描述 |
| coverImg | String | 封面图 URL |
| type | String | `GOODS` / `SERVICE` |
| supportDelivery | Boolean | 是否支持配送 |
| skus | List\<SkuPrice\> | SKU 列表（见下方） |

**SkuPrice**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | SKU ID |
| specName | String | 规格名称，如 `"5kg"` / `"中型犬（10-25kg）"` |
| price | String | 原价 |
| dealPrice | String | 当前用户成交价（已根据会员等级计算） |

**业务规则**：
- 仅返回在售商品（`status = ON_SALE`）
- `dealPrice` 根据当前登录用户的会员等级实时计算

---

### 3. 会员信息

#### GET /api/app/member/profile

获取当前登录用户的会员信息。

**请求体**：无（通过 cookie 识别用户）

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| isMember | Boolean | 是否为会员 |
| memberLevel | Object \| null | 会员等级信息（非会员时为 null） |
| memberLevel.name | String | 等级名称，如 `"2000档会员"` |
| memberLevel.discountRate | String | 服务折扣率，如 `"0.85"` |
| serviceDiscountText | String \| null | 折扣描述，如 `"8.5折"`（非会员时为 null） |

---

### 4. 购物车价格预览

#### POST /api/app/cart/calculate

传入购物车 items，返回各 item 价格明细 + 汇总 + 配送校验结果。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| items | List\<CartItem\> | 是 | 购物车项列表 |
| deliveryLat | String | 否 | 配送地址纬度（勾选配送时必填） |
| deliveryLng | String | 否 | 配送地址经度（勾选配送时必填） |

**CartItem**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productId | Long | 是 | 商品 ID |
| skuId | Long | 否 | SKU ID（无规格商品可不传） |
| quantity | Integer | 是 | 数量，≥1 |

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List\<CalculatedItem\> | 各 item 价格明细 |
| goodsAmount | String | 用品金额合计（已折扣） |
| serviceAmount | String | 服务金额合计（已折扣） |
| deliveryFee | String \| null | 配送费（按系统策略计算；不配送时为 `null`） |
| totalAmount | String | 合计金额 |
| deliveryCheck | Object | 配送校验结果（见下方） |

**CalculatedItem**：

| 字段 | 类型 | 说明 |
|------|------|------|
| productId | Long | 商品 ID |
| skuId | Long \| null | SKU ID |
| productName | String | 商品名称 |
| skuName | String \| null | 规格名称 |
| type | String | `GOODS` / `SERVICE` |
| originalPrice | String | 原价单价 |
| dealPrice | String | 成交价单价（已根据会员等级计算） |
| quantity | Integer | 数量 |
| subtotal | String | 小计（dealPrice × quantity） |

**DeliveryCheck**：

| 字段 | 类型 | 说明 |
|------|------|------|
| canDeliver | Boolean | 是否可以配送（购物车中是否有可配送用品） |
| deliverableGoodsOriginal | String | 可配送用品的原价合计 |
| minAmount | String | 起送金额（如 `"20.00"`） |
| reachedMinAmount | Boolean | 是否达到起送门槛 |
| gap | String \| null | 距起送还差多少（未达到时） |
| deliveryDistanceMeter | Long \| null | 配送距离（米），仅传入坐标时返回 |
| deliveryDistanceText | String \| null | 距离描述，如 `"2.3km"` |

---

### 5. 订单

#### POST /api/app/orders

下单。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| items | List\<CartItem\> | 是 | 购物车项列表（同 cart/calculate 的 CartItem） |
| customerName | String | 否 | 联系人姓名 |
| remark | String | 否 | 备注 |
| needDelivery | Boolean | 否 | 是否需要配送，默认 false |
| deliveryLat | String | 条件必填 | 配送纬度（needDelivery=true 时必填） |
| deliveryLng | String | 条件必填 | 配送经度（needDelivery=true 时必填） |
| deliveryAddress | String | 条件必填 | 配送详细地址（needDelivery=true 时必填） |

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| orderNo | String | 订单号 |
| totalAmount | String | 合计金额 |
| goodsAmount | String | 用品金额 |
| serviceAmount | String | 服务金额 |
| deliveryFee | String | 配送费 |
| deliveryDistanceMeter | Long \| null | 配送距离（米） |
| deliveryDistanceText | String \| null | 配送距离描述，如 `"2.3km"` |

**业务规则**：
- 后端权威算价，不信任前端传入价格
- 金额精度：单价 HALF_UP 四舍五入到分后再乘数量
- 配送校验：可配送用品原价合计 ≥ 起送金额
- 运费按系统配置策略计算（FREE / FIXED / TIERED）
- 下单成功后异步推企微通知

---

#### GET /api/app/orders

我的订单列表（分页）。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 10 |

**响应 data**：`Paginated<OrderListItem>`

**OrderListItem**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |
| orderNo | String | 订单号 |
| totalAmount | String | 合计金额 |
| goodsAmount | String | 用品金额 |
| serviceAmount | String | 服务金额 |
| needDelivery | Boolean | 是否配送 |
| createTime | String | 下单时间，如 `"2026-04-23 14:32"` |
| itemCount | Integer | 商品项数量 |
| summaryText | String | 摘要，如 `"金毛粮5kg ×1 等3件"` |

---

#### GET /api/app/orders/{id}

订单详情。

**Path 参数**：`id`（订单 ID）

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |
| orderNo | String | 订单号 |
| customerPhone | String | 脱敏手机号 |
| customerName | String \| null | 联系人姓名 |
| memberLevelSnapshot | String \| null | 下单时会员等级快照 |
| goodsAmount | String | 用品金额 |
| serviceAmount | String | 服务金额 |
| deliveryFee | String | 配送费 |
| totalAmount | String | 合计金额 |
| needDelivery | Boolean | 是否配送 |
| deliveryAddress | String \| null | 配送地址 |
| deliveryDistanceMeter | Long \| null | 配送距离（米） |
| deliveryDistanceText | String \| null | 配送距离描述 |
| remark | String \| null | 备注 |
| createTime | String | 下单时间 |
| items | List\<OrderItemDetail\> | 订单明细列表 |

**业务规则**：
- 仅允许查询当前登录用户自己的订单，越权访问返回 403

**OrderItemDetail**：

| 字段 | 类型 | 说明 |
|------|------|------|
| productName | String | 商品名称 |
| skuName | String \| null | 规格名称 |
| type | String | `GOODS` / `SERVICE` |
| originalPrice | String | 原价 |
| dealPrice | String | 成交价 |
| quantity | Integer | 数量 |
| subtotal | String | 小计 |

---

## 二、管理端接口（/api/admin）

> 管理端所有接口需登录后台账号，根据角色鉴权。下方每个接口标注所需角色。

---

### 1. 认证

#### POST /api/admin/auth/login

后台账号登录。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 账号 ID |
| username | String | 用户名 |
| realName | String | 姓名 |
| role | String | 角色：`BOSS` / `MANAGER` / `STAFF` |
| roleLabel | String | 角色中文名 |

---

#### POST /api/admin/auth/logout

后台退出登录。

**响应 data**：`null`

---

#### GET /api/admin/auth/profile

获取当前登录后台账号信息。

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 账号 ID |
| username | String | 用户名 |
| realName | String | 姓名 |
| role | String | 角色 |
| roleLabel | String | 角色中文名，如 `"老板"` / `"店长"` / `"店员"` |

---

### 2. 分类管理

> 权限：BOSS + MANAGER

#### GET /api/admin/categories

分类列表（不分页）。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 否 | `GOODS` / `SERVICE` |

**响应 data**：`List<CategoryDetail>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 分类 ID |
| name | String | 分类名称 |
| icon | String \| null | 图标 URL |
| type | String | `GOODS` / `SERVICE` |
| sort | Integer | 排序值 |
| productCount | Integer | 该分类下商品数量 |

---

#### POST /api/admin/categories

新增分类。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 分类名称 |
| icon | String | 否 | 图标 URL |
| type | String | 是 | `GOODS` / `SERVICE` |
| sort | Integer | 否 | 排序值，默认 0 |

**响应 data**：新增的 `CategoryDetail`（同上）

---

#### PUT /api/admin/categories/{id}

编辑分类。

**Path 参数**：`id`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 否 | 分类名称 |
| icon | String | 否 | 图标 URL |
| sort | Integer | 否 | 排序值 |

**响应 data**：更新后的 `CategoryDetail`

**业务规则**：
- `type` 不可修改（分类类型创建后不可变）

---

#### DELETE /api/admin/categories/{id}

删除分类。

**Path 参数**：`id`

**响应 data**：`null`

**业务规则**：
- 该分类下有商品时禁止删除，返回 400

---

### 3. 商品管理

> 权限：BOSS + MANAGER

#### GET /api/admin/products

商品列表（分页）。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |
| keyword | String | 否 | 搜索关键词（商品名称） |
| categoryId | Long | 否 | 按分类筛选 |
| type | String | 否 | `GOODS` / `SERVICE` |
| status | String | 否 | `ON_SALE` / `OFF_SALE` |

**响应 data**：`Paginated<ProductListItem>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 商品 ID |
| name | String | 商品名称 |
| coverImg | String \| null | 封面图 URL |
| categoryName | String | 所属分类名称 |
| type | String | `GOODS` / `SERVICE` |
| status | String | `ON_SALE` / `OFF_SALE` |
| supportDelivery | Boolean | 是否支持配送 |
| sort | Integer | 排序值 |
| skuCount | Integer | SKU 数量 |
| minPrice | String | 最低原价 |
| createTime | String | 创建时间 |

---

#### POST /api/admin/products

新增商品。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Long | 是 | 所属分类 ID |
| name | String | 是 | 商品名称 |
| description | String | 否 | 商品描述 |
| coverImg | String | 否 | 封面图 URL |
| type | String | 是 | `GOODS` / `SERVICE`（必须与分类 type 一致） |
| supportDelivery | Boolean | 否 | 是否支持配送，默认 false（SERVICE 强制 false） |
| sort | Integer | 否 | 排序值，默认 0 |
| skus | List\<SkuInput\> | 否 | SKU 列表 |

**SkuInput**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| specName | String | 是 | 规格名称 |
| price | String | 是 | 原价 |
| memberPrice | String | 否 | 会员价（GOODS 可选，SERVICE 忽略） |
| sort | Integer | 否 | 排序值 |

**响应 data**：新增的完整商品对象（含 ID、SKU 列表）

---

#### GET /api/admin/products/{id}

商品详情（含 SKU 列表）。

**Path 参数**：`id`

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 商品 ID |
| categoryId | Long | 分类 ID |
| categoryName | String | 分类名称 |
| name | String | 商品名称 |
| description | String \| null | 描述 |
| coverImg | String \| null | 封面图 URL |
| type | String | `GOODS` / `SERVICE` |
| status | String | `ON_SALE` / `OFF_SALE` |
| supportDelivery | Boolean | 是否支持配送 |
| sort | Integer | 排序值 |
| createTime | String | 创建时间 |
| skus | List\<SkuDetail\> | SKU 列表 |

**SkuDetail**：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | SKU ID |
| specName | String | 规格名称 |
| price | String | 原价 |
| memberPrice | String \| null | 会员价 |
| sort | Integer | 排序值 |

---

#### PUT /api/admin/products/{id}

编辑商品基本信息 + 批量更新 SKU。

**Path 参数**：`id`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 否 | 商品名称 |
| description | String | 否 | 描述 |
| coverImg | String | 否 | 封面图 URL |
| supportDelivery | Boolean | 否 | 是否支持配送 |
| sort | Integer | 否 | 排序值 |
| skus | List\<SkuInput\> | 否 | SKU 列表（传则全量替换） |

**响应 data**：更新后的完整商品对象

**业务规则**：
- SKU 全量替换：传入的 SKU 列表会替换原有所有 SKU
- SKU 价格变更会记录操作日志
- 已下架的商品也可编辑

---

#### PUT /api/admin/products/{id}/status

商品上架/下架。

**Path 参数**：`id`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 是 | `ON_SALE` / `OFF_SALE` |

**响应 data**：`null`

---

#### DELETE /api/admin/products/{id}

删除商品。

**Path 参数**：`id`

**响应 data**：`null`

**业务规则**：
- 已被订单引用的商品禁止删除（通过 order_item 关联判断），建议用下架代替删除

---

### 4. 会员等级管理

> 权限：BOSS + MANAGER

#### GET /api/admin/member-levels

会员等级列表（不分页）。

**响应 data**：`List<MemberLevel>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 等级 ID |
| name | String | 等级名称，如 `"500档会员"` |
| discountRate | String | 服务折扣率，如 `"0.95"` |
| sort | Integer | 排序值 |
| status | String | `ENABLED` / `DISABLED` |
| memberCount | Integer | 该等级下会员数量 |

---

#### POST /api/admin/member-levels

新增会员等级。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 等级名称 |
| discountRate | String | 是 | 服务折扣率，如 `"0.85"` |
| sort | Integer | 否 | 排序值，默认 0 |

**响应 data**：新增的 `MemberLevel`

---

#### PUT /api/admin/member-levels/{id}

编辑会员等级。

**Path 参数**：`id`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 否 | 等级名称 |
| discountRate | String | 否 | 折扣率 |
| sort | Integer | 否 | 排序值 |

**响应 data**：更新后的 `MemberLevel`

---

#### PUT /api/admin/member-levels/{id}/status

启停用会员等级。

**Path 参数**：`id`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 是 | `ENABLED` / `DISABLED` |

**响应 data**：`null`

**业务规则**：
- 有关联会员时禁止停用，返回 400

---

#### DELETE /api/admin/member-levels/{id}

删除会员等级。

**Path 参数**：`id`

**响应 data**：`null`

**业务规则**：
- 有关联会员时禁止删除，返回 400

---

### 5. 会员管理

> 权限：BOSS + MANAGER

#### GET /api/admin/members

会员列表（分页）。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |
| keyword | String | 否 | 搜索关键词（姓名或手机号） |
| levelId | Long | 否 | 按等级筛选 |

**响应 data**：`Paginated<MemberListItem>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 会员 ID |
| name | String | 姓名 |
| phones | List\<String\> | 手机号列表 |
| levelId | Long | 等级 ID |
| levelName | String | 等级名称 |
| remark | String \| null | 备注 |
| createTime | String | 创建时间 |

---

#### POST /api/admin/members

新增会员。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 姓名 |
| phones | List\<String\> | 是 | 手机号列表（至少 1 个，且全局唯一） |
| levelId | Long | 是 | 会员等级 ID |
| remark | String | 否 | 备注 |

**响应 data**：新增的 `MemberListItem`

**业务规则**：
- 手机号不可重复（跨所有会员全局唯一）

---

#### PUT /api/admin/members/{id}

编辑会员信息。

**Path 参数**：`id`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 否 | 姓名 |
| phones | List\<String\> | 否 | 手机号列表（全局唯一） |
| levelId | Long | 否 | 等级 ID |
| remark | String | 否 | 备注 |

**响应 data**：更新后的 `MemberListItem`

**业务规则**：
- 修改手机号列表时，后端需校验全局唯一

---

#### DELETE /api/admin/members/{id}

删除会员。

**Path 参数**：`id`

**响应 data**：`null`

---

### 6. 订单管理

> 权限：查看 — 全角色；标记已处理 — 全角色

#### GET /api/admin/orders

订单列表（分页）。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |
| keyword | String | 否 | 搜索（订单号 / 手机号 / 联系人） |
| processed | Boolean | 否 | 按处理状态筛选 |
| needDelivery | Boolean | 否 | 是否配送订单 |
| startTime | String | 否 | 起始时间，如 `"2026-04-01"` |
| endTime | String | 否 | 结束时间，如 `"2026-04-30"` |

**响应 data**：`Paginated<AdminOrderListItem>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单 ID |
| orderNo | String | 订单号 |
| customerPhone | String | 脱敏手机号 |
| customerName | String \| null | 联系人 |
| memberLevelSnapshot | String \| null | 会员等级快照 |
| goodsAmount | String | 用品金额 |
| serviceAmount | String | 服务金额 |
| deliveryFee | String | 配送费 |
| totalAmount | String | 合计 |
| needDelivery | Boolean | 是否配送 |
| deliveryDistanceMeter | Long \| null | 配送距离（米） |
| deliveryDistanceText | String \| null | 配送距离描述 |
| processed | Boolean | 是否已处理 |
| remark | String \| null | 备注 |
| createTime | String | 下单时间 |

---

#### GET /api/admin/orders/{id}

订单详情。

**Path 参数**：`id`

**响应 data**：同 C 端订单详情结构，额外增加以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| customerPhoneRaw | String | 完整手机号（管理端可见） |
| deliveryAddress | String \| null | 完整配送地址 |
| deliveryLat | String \| null | 纬度 |
| deliveryLng | String \| null | 经度 |
| deliveryDistanceMeter | Long \| null | 配送距离（米） |
| deliveryDistanceText | String \| null | 配送距离描述 |

---

#### PUT /api/admin/orders/{id}/processed

切换订单处理状态。

**Path 参数**：`id`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| processed | Boolean | 是 | `true` = 已处理，`false` = 未处理 |

**响应 data**：`null`

---

### 7. 系统配置

> 权限：仅 BOSS

#### GET /api/admin/system-config

获取系统配置（结构化）与最近变更记录。

**响应 data**：`SystemConfigResponse`

| 字段 | 类型 | 说明 |
|------|------|------|
| config | Object | 系统配置 |
| config.shopLat | String \| null | 店铺纬度 |
| config.shopLng | String \| null | 店铺经度 |
| config.deliveryRadiusKm | Number | 配送半径（km） |
| config.deliveryMinAmount | String | 起送价 |
| config.deliveryFeeType | String | `FREE` / `TIERED`（暂不实现 FIXED） |
| config.fixedDeliveryFee | String | 固定运费（保留字段，当前不使用） |
| config.tieredDeliveryFeeRules | List\<TierRule\> | 分段运费规则（`TIERED` 时生效） |
| config.orderTimeEnabled | Boolean | 是否启用接单时段 |
| config.orderStartTime | String | 接单开始时间（`HH:mm`） |
| config.orderEndTime | String | 接单结束时间（`HH:mm`） |
| config.qywxWebhookUrl | String \| null | 企微 Webhook（建议脱敏返回 `key=***`） |
| config.hasQywxWebhook | Boolean | 是否已配置 Webhook |
| config.updatedBy | String | 最近更新人 |
| config.updatedAt | String | 最近更新时间 |
| changeLogs | List\<SystemConfigChangeLog\> | 变更记录（最近 N 条） |

---

#### PUT /api/admin/system-config

更新系统配置。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| shopLat | String | 否 | 店铺纬度 |
| shopLng | String | 否 | 店铺经度 |
| deliveryRadiusKm | Number | 是 | 配送半径（km） |
| deliveryMinAmount | String | 是 | 起送价 |
| deliveryFeeType | String | 是 | `FREE` / `TIERED`（暂不实现 FIXED） |
| fixedDeliveryFee | String | 是 | 固定运费 |
| tieredDeliveryFeeRules | List\<TierRule\> | 是 | 分段规则 |
| orderTimeEnabled | Boolean | 是 | 是否启用接单时段 |
| orderStartTime | String | 是 | 开始时间（`HH:mm`） |
| orderEndTime | String | 是 | 结束时间（`HH:mm`） |
| qywxWebhookUrl | String | 否 | webhook 地址；缺失=不修改，空串=清空 |

**响应 data**：更新后的 `SystemConfig`

**业务规则**：
- 修改后立即生效
- 修改会记录操作日志
- 后端必须校验 Webhook 白名单，防 SSRF

---

#### POST /api/admin/system-config/test-webhook

发送 Webhook 测试消息。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| webhookUrl | String | 是 | 完整 webhook 地址 |

**响应 data**：`null` 或测试结果对象

---

### 8. 账号管理

> 权限：仅 BOSS

#### GET /api/admin/users

后台账号列表（不分页）。

**响应 data**：`List<AdminUser>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 账号 ID |
| username | String | 用户名 |
| realName | String | 姓名 |
| role | String | `BOSS` / `MANAGER` / `STAFF` |
| roleLabel | String | 角色中文名 |
| status | String | `ENABLED` / `DISABLED` |

---

#### POST /api/admin/users

新增后台账号。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名（唯一） |
| password | String | 是 | 密码 |
| realName | String | 是 | 姓名 |
| role | String | 是 | `MANAGER` / `STAFF` |

**响应 data**：新增的 `AdminUser`

**业务规则**：
- BOSS 账号只能有一个，新增时 role 不可传 `BOSS`

---

#### PUT /api/admin/users/{id}

编辑后台账号。

**Path 参数**：`id`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| realName | String | 否 | 姓名 |
| role | String | 否 | 角色 |

**响应 data**：更新后的 `AdminUser`

---

#### PUT /api/admin/users/{id}/status

启用/禁用后台账号。

**Path 参数**：`id`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 是 | `ENABLED` / `DISABLED` |

**响应 data**：`null`

**业务规则**：
- 禁用后该账号立即无法登录
- BOSS 账号不可被禁用

---

#### PUT /api/admin/users/{id}/password

重置密码。

**Path 参数**：`id`

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| newPassword | String | 是 | 新密码 |

**响应 data**：`null`

---

#### DELETE /api/admin/users/{id}

删除后台账号。

**Path 参数**：`id`

**响应 data**：`null`

**业务规则**：
- BOSS 账号不可删除

---

### 9. 数据统计

> 权限：BOSS + MANAGER

#### GET /api/admin/stats/overview

总览数据。

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| todayOrderCount | Integer | 今日订单数 |
| todayAmount | String | 今日销售额 |
| totalOrderCount | Integer | 总订单数 |
| totalAmount | String | 总销售额 |
| totalMemberCount | Integer | 会员总数 |
| unprocessedCount | Integer | 未处理订单数 |

---

#### GET /api/admin/stats/orders

订单统计（趋势图数据）。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| period | String | 是 | `DAY`（近 30 天按天）/ `WEEK`（近 12 周按周）/ `MONTH`（近 12 月按月） |

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List\<StatsItem\> | 统计数据点列表 |

**StatsItem**：

| 字段 | 类型 | 说明 |
|------|------|------|
| label | String | 时间标签，如 `"04-20"` / `"第15周"` / `"2026-03"` |
| orderCount | Integer | 订单数 |
| amount | String | 销售额 |

---

#### GET /api/admin/stats/members/ranking

会员消费排行。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 返回条数，默认 10 |

**响应 data**：`List<MemberRanking>`

| 字段 | 类型 | 说明 |
|------|------|------|
| memberId | Long | 会员 ID |
| name | String | 会员姓名 |
| phone | String | 脱敏手机号 |
| levelName | String | 等级名称 |
| orderCount | Integer | 订单数 |
| totalAmount | String | 累计消费金额 |

---

### 10. 操作日志

#### GET /api/admin/logs

操作日志列表（分页）。

**Query 参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认 1 |
| size | Integer | 否 | 每页条数，默认 20 |
| userId | Long | 否 | 按操作人筛选 |
| action | String | 否 | 按操作类型筛选 |
| startTime | String | 否 | 起始时间 |
| endTime | String | 否 | 结束时间 |

**响应 data**：`Paginated<OperationLog>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 日志 ID |
| userId | Long | 操作人 ID |
| username | String | 操作人用户名 |
| action | String | 操作类型，如 `"UPDATE_PRODUCT"` / `"UPDATE_CONFIG"` |
| target | String | 操作目标，如 `"商品:金毛粮5kg"` |
| before | String \| null | 修改前值（JSON） |
| after | String \| null | 修改后值（JSON） |
| time | String | 操作时间 |

---

### 11. 文件上传

> 权限：BOSS + MANAGER

#### POST /api/admin/files/upload

上传图片。

**请求体**：`multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 图片文件（jpg/png/webp，最大 5MB） |

**响应 data**：

| 字段 | 类型 | 说明 |
|------|------|------|
| url | String | 图片访问 URL |
| key | String | 文件存储 key（用于删除） |

---

#### DELETE /api/admin/files/{key}

删除图片。

**Path 参数**：`key`（上传时返回的 key）

**响应 data**：`null`

---

### 12. WebSocket

#### WS /api/admin/ws/orders

新订单实时推送（管理端连接）。

**连接方式**：WebSocket，通过 cookie 鉴权。

**推送消息格式**：

```json
{
  "type": "NEW_ORDER",
  "data": {
    "orderNo": "PS20260423001",
    "customerName": "张先生",
    "customerPhone": "138****8888",
    "memberLevelSnapshot": "2000档会员",
    "totalAmount": "220.00",
    "needDelivery": true,
    "deliveryDistanceMeter": 2300,
    "deliveryDistanceText": "2.3km",
    "createTime": "2026-04-23 14:32:00",
    "itemSummary": "金毛粮5kg ×1, 中型犬洗澡 ×1"
  }
}
```

---

## 三、接口总览

### C 端（/api/app）

| 方法 | URL | 说明 | 需登录 |
|------|-----|------|--------|
| POST | /api/app/auth/sms-code | 发送验证码 | 否 |
| POST | /api/app/auth/login | 登录 | 否 |
| POST | /api/app/auth/logout | 退出 | 是 |
| GET | /api/app/auth/check | 检查登录态 | 否 |
| GET | /api/app/categories | 分类列表 | 否 |
| GET | /api/app/products | 按类型获取商品列表 | 否 |
| GET | /api/app/categories/{id}/products | 分类下商品 | 否 |
| GET | /api/app/products/{id} | 商品详情 | 否 |
| GET | /api/app/member/profile | 会员信息 | 是 |
| POST | /api/app/cart/calculate | 购物车价格预览 | 是 |
| POST | /api/app/orders | 下单 | 是 |
| GET | /api/app/orders | 我的订单列表 | 是 |
| GET | /api/app/orders/{id} | 订单详情 | 是 |

### 管理端（/api/admin）

| 方法 | URL | 说明 | 权限 |
|------|-----|------|------|
| POST | /api/admin/auth/login | 后台登录 | - |
| POST | /api/admin/auth/logout | 后台退出 | 已登录 |
| GET | /api/admin/auth/profile | 当前账号信息 | 已登录 |
| GET | /api/admin/categories | 分类列表 | BOSS/MANAGER |
| POST | /api/admin/categories | 新增分类 | BOSS/MANAGER |
| PUT | /api/admin/categories/{id} | 编辑分类 | BOSS/MANAGER |
| DELETE | /api/admin/categories/{id} | 删除分类 | BOSS/MANAGER |
| GET | /api/admin/products | 商品列表 | BOSS/MANAGER |
| POST | /api/admin/products | 新增商品 | BOSS/MANAGER |
| GET | /api/admin/products/{id} | 商品详情 | BOSS/MANAGER |
| PUT | /api/admin/products/{id} | 编辑商品 | BOSS/MANAGER |
| PUT | /api/admin/products/{id}/status | 上下架 | BOSS/MANAGER |
| DELETE | /api/admin/products/{id} | 删除商品 | BOSS/MANAGER |
| GET | /api/admin/member-levels | 等级列表 | BOSS/MANAGER |
| POST | /api/admin/member-levels | 新增等级 | BOSS/MANAGER |
| PUT | /api/admin/member-levels/{id} | 编辑等级 | BOSS/MANAGER |
| PUT | /api/admin/member-levels/{id}/status | 启停用等级 | BOSS/MANAGER |
| DELETE | /api/admin/member-levels/{id} | 删除等级 | BOSS/MANAGER |
| GET | /api/admin/members | 会员列表 | BOSS/MANAGER |
| POST | /api/admin/members | 新增会员 | BOSS/MANAGER |
| PUT | /api/admin/members/{id} | 编辑会员 | BOSS/MANAGER |
| DELETE | /api/admin/members/{id} | 删除会员 | BOSS/MANAGER |
| GET | /api/admin/orders | 订单列表 | ALL |
| GET | /api/admin/orders/{id} | 订单详情 | ALL |
| PUT | /api/admin/orders/{id}/processed | 标记已处理 | ALL |
| GET | /api/admin/system-config | 系统配置 | BOSS |
| PUT | /api/admin/system-config | 更新配置 | BOSS |
| POST | /api/admin/system-config/test-webhook | 测试 webhook | BOSS |
| GET | /api/admin/users | 账号列表 | BOSS |
| POST | /api/admin/users | 新增账号 | BOSS |
| PUT | /api/admin/users/{id} | 编辑账号 | BOSS |
| PUT | /api/admin/users/{id}/status | 启停用账号 | BOSS |
| PUT | /api/admin/users/{id}/password | 重置密码 | BOSS |
| DELETE | /api/admin/users/{id} | 删除账号 | BOSS |
| GET | /api/admin/stats/overview | 总览数据 | BOSS/MANAGER |
| GET | /api/admin/stats/orders | 订单趋势 | BOSS/MANAGER |
| GET | /api/admin/stats/members/ranking | 会员排行 | BOSS/MANAGER |
| GET | /api/admin/logs | 操作日志 | ALL |
| POST | /api/admin/files/upload | 上传图片 | BOSS/MANAGER |
| DELETE | /api/admin/files/{key} | 删除图片 | BOSS/MANAGER |
| WS | /api/admin/ws/orders | 新订单推送 | ALL |

# SKU 独立图片 - 实施方案

> 📋 **状态**：已实施（2026-09-04 本地编码完成并端到端验证通过，待随下次部署上线）
> 📅 **定稿日期**：2026-07-16　**实施日期**：2026-09-04
> 🎯 **目标**：让每个 SKU 拥有独立图片，H5 选规格时顶部大图随之切换；商品主图保留为兜底；购物车与订单明细显示对应 SKU 图（订单走快照，不随后续改图变化）。
> 📎 本文为实施手册，改动点附文件名与行号。行号为撰写时的代码现状，实施时以最新代码为准。

## 背景与目标

当前商品与服务只有一张主图 `product.cover_img`，所有 SKU 共用。参考电商常见效果，需要做到：每个 SKU 可配置独立图片，用户在 H5 选择规格时，顶部大图切换为该 SKU 的图片；SKU 未配图时回退到商品主图。同时购物车与订单明细也显示对应 SKU 的图片。

一个关键约束决定了订单侧的实现方式：Admin 端 SKU 采用「先 `deleteByProductId` 再 `insertBatch`」的覆盖式更新（见 `ProductServiceImpl.update`），每次保存商品都会重建 SKU，`sku.id` 随之变化。因此订单不能通过 `sku_id` 反查 `sku.img_url`，必须在下单时把 SKU 图片**快照**写入 `order_item`。

另一个现状补充：H5 的购物车（`CartDrawer.vue`）与订单详情（`OrderDetailPage.vue`）当前没有商品图展示位，本次属于「新增缩略图展示」，而非替换。图片上传机制（`POST /api/admin/files/upload`，存相对 URL `/uploads/...`）已就绪，直接复用，无需改动。

## 需求边界确认

以下决策点已与需求方逐条确认。

| 维度 | 决策 | 备注 |
|---|---|---|
| **H5 展示形态** | 仿参考图：弹窗顶部大图 + 下方缩略图规格卡片 | 选规格即切大图 |
| **主图兜底** | 商品 `cover_img` 保留 | SKU 无图时回退主图；列表卡片仍用主图 |
| **购物车图** | 改用 SKU 图（无图回退主图） | 由 `CartItem.skuImg` 本地持久化缓存 |
| **订单图** | `order_item` 落 `sku_img` 快照 | 因 SKU 覆盖式更新致 `sku.id` 变化，必须快照 |
| **适用范围** | GOODS 与 SERVICE 均支持 SKU 图 | 单 SKU 商品与服务同样适用 |
| **图片存储** | 复用现有 `POST /api/admin/files/upload` | 落库为 `/uploads/...` 相对 URL |
| **字段命名** | `sku.img_url` / `order_item.sku_img` | Java 字段 `imgUrl` / `skuImg` |
| **不改动的部分** | 列表卡片、订单列表摘要、购物车算价接口 | 见各模块说明 |

## 数据流

改动贯通「上传 → 展示 → 下单快照」全链路。

```
Admin 上传 → sku.img_url（DB）
   ├─ H5 详情接口透出 sku.imgUrl → SkuSelectorPopup 选规格切大图
   ├─ 加购缓存 → CartItem.skuImg → CartDrawer 显示
   └─ PriceCalculationServiceImpl 取 sku.imgUrl → CalculatedItemResult.skuImg
        → OrderServiceImpl 写 order_item.sku_img（快照）
        → buildDetailMap 透出 → OrderDetailPage 显示
        （历史订单不随后续改图变化）
```

## 一、数据库

为新部署与已部署环境分别处理。

**1. 更新 `backend/sql/init.sql`**，让新部署原生带列。

- 在 `CREATE TABLE sku`（约 L31-43）增加一列，置于 `member_price` 之后：

  ```sql
  img_url VARCHAR(255) NULL COMMENT 'SKU图片URL',
  ```

- 在 `CREATE TABLE order_item`（约 L128-143）增加一列，置于 `sku_name` 之后：

  ```sql
  sku_img VARCHAR(255) NULL COMMENT '下单时SKU图片快照',
  ```

**2. 新建迁移脚本 `backend/sql/migration_sku_img_v1.sql`**，供已部署环境增量执行。风格仿 `migration_ad_v1.sql`。

```sql
SET NAMES utf8mb4;

ALTER TABLE sku
    ADD COLUMN img_url VARCHAR(255) NULL COMMENT 'SKU图片URL' AFTER member_price;

ALTER TABLE order_item
    ADD COLUMN sku_img VARCHAR(255) NULL COMMENT '下单时SKU图片快照' AFTER sku_name;
```

## 二、后端（Spring Boot + MyBatis）

按下表逐文件修改。查询层中，`SkuMapper.xml` 的 `selectByProductId` 与 `selectById` 使用 `SELECT *`，只要 `Sku` 实体新增字段即可自动映射；写入层与显式列名的查询需要手动补列。

| 文件 | 改动说明 |
|---|---|
| `entity/Sku.java` | 新增字段 `private String imgUrl;` |
| `entity/OrderItem.java` | 新增字段 `private String skuImg;` |
| `mapper/SkuMapper.xml`（L14-25） | `insert` 与 `insertBatch` 的列清单和 VALUES 同步增加 `img_url`，分别对应 `#{imgUrl}` 与 `#{item.imgUrl}` |
| `mapper/ProductMapper.xml`（L56-72） | `selectById` 的 SELECT 增加 `s.img_url AS sku_img_url`；`productDetailResultMap` 的 `<collection>`（L23-34）增加 `<result property="imgUrl" column="sku_img_url"/>` |
| `mapper/OrderItemMapper.xml` | `insertBatch` 的列清单与 `foreach` 增加 `sku_img`（`#{item.skuImg}`）；`selectByOrderId` 的 SELECT 列表增加 `sku_img` |
| `service/dto/CalculatedItemResult.java` | 新增字段 `private String skuImg;` |
| `service/impl/PriceCalculationServiceImpl.java`（约 L93） | 构造 `CalculatedItemResult` 处增加 `result.setSkuImg(sku.getImgUrl());`（`sku` 在 L62-93 已匹配得到） |
| `service/impl/OrderServiceImpl.java`（约 L198） | 构造 `OrderItem` 处增加 `oi.setSkuImg(ci.getSkuImg());` |
| `service/impl/OrderServiceImpl.java` `buildDetailMap`（L339-348） | 订单项 VO 增加 `m.put("skuImg", item.getSkuImg() != null ? item.getSkuImg() : "");`，H5 与 Admin 共用此 VO |
| `controller/admin/AdminProductController.java` | 内部类 `SkuRequest`（L209-218）增加 `private String imgUrl;`；`toSku()`（L136-145）增加 `sku.setImgUrl(s.getImgUrl());`；`toSkuMap()`（L181-190）增加 `"imgUrl", s.getImgUrl() != null ? s.getImgUrl() : ""` |
| `controller/app/AppProductController.java` `getDetail`（L82-88） | SKU 的 `Map.of(...)` 增加 `"imgUrl", s.getImgUrl() != null ? s.getImgUrl() : ""`。注意 `Map.of` 不接受 null，必须空串兜底；当前 5 对键值升级为 6 对，未超过 10 对上限 |

> **提示**：`ProductServiceImpl.update` 的「先删后插」会天然带上 `imgUrl`，无需额外处理；旧订单因 `order_item.sku_img` 快照存在而不受影响。

## 三、Admin 前端（Vue 3 + Element Plus）

在商品编辑弹窗的「规格管理」行内为每个 SKU 增加图片上传单元格。

**1. 更新类型定义 `frontend/admin/src/types/index.ts`**，在 `SkuDetail`（L46-53）增加 `imgUrl?: string | null`。

**2. 更新 `frontend/admin/src/views/ProductPage.vue`**。

- 在 `addSkuRow()`（L95-105）的默认对象中增加 `imgUrl: ''`。
- 在 `.sku-columns`（L430-437）增加列头 `<span style="width: 90px">SKU图</span>`。该列对 GOODS 与 SERVICE 始终显示。
- 在 `.sku-row`（L440-462）增加一个 `el-upload` 单元格，属性 `:show-file-list="false"`、`:http-request`、`accept="image/*"`。新增按行上传函数 `handleSkuUpload(options, sku)`：调用 `uploadFile(options.file)`（来自 `src/api/file.ts`），成功后将 `sku.imgUrl` 置为 `res.data.url`。写法参考封面图的 `handleUpload`（L82-93）与模板（L393-405）。
- 已有图时显示 `el-avatar`（`:src="sku.imgUrl"` `:size="56"` `shape="square"`）并叠加一个删除小按钮用于清空 `sku.imgUrl`；无图时显示 `Plus` 占位。
- 在 `handleProductSubmit` 的提交映射（L187-193）增加 `imgUrl: s.imgUrl`。
- 编辑回填（L141，使用 `...s` 展开）会自动带回 `imgUrl`，前提是后端 `toSkuMap` 已返回该字段。

> **提示**：编辑弹窗宽 720 px，新增约 90 px 列后总宽约 580 px，不会溢出；如遇拥挤可微调其余列宽。

## 四、H5 前端（Vue 3 + Vant）

### 1. 类型定义 `frontend/h5/src/types/index.ts`

- `SkuPrice`（L31-37）增加 `imgUrl: string | null`。
- `CartItem`（L62-73）增加 `skuImg: string | null`。
- `OrderItemDetail`（L155-163）增加 `skuImg: string | null`。

### 2. 重构 SKU 选择弹窗 `frontend/h5/src/components/product/SkuSelectorPopup.vue`

参考目标效果图，把弹窗从「小图头部 + 文字规格」改造为「顶部大图 + 缩略图规格」。

- 顶部改为**大图区**（满宽，`aspect-ratio: 1/1`，圆角）。展示图片取 `selectedSku?.imgUrl || product?.coverImg`；两者都无时显示占位图标，沿用 `product.type === 'SERVICE' ? '✂️' : '🦴'`。
- 大图下方保留商品名、价格（`selectedSku?.dealPrice ?? skus[0]?.dealPrice`）、描述，从原 `sku-header` 区域迁移。
- 规格区（L23-37）由纯文字按钮改为**缩略图卡片**。每张卡片包含小缩略图（`sku.imgUrl || product?.coverImg`，无图占位，约 56 px）、规格名、价格；选中态复用 `.sku-option.active` 样式；点击触发 `selectSku` 并切换顶部大图。布局使用 `flex-wrap` 横排。
- 数量 stepper 与加购按钮保持不变。
- 在 `handleAddToCart`（L99-117）中，`CartItem` 入参增加 `skuImg: sku.imgUrl || props.product.coverImg`，同时保留原 `productCoverImg` 作为回退。
- 弹窗 `maxHeight` 视布局需要调整为 80 vh，内部滚动。

### 3. 购物车 `frontend/h5/src/components/common/CartDrawer.vue`

当前购物车行无图。将 `.cart-item`（L17-39）布局改为 `[缩略图 64 px] [信息 flex:1] [stepper]`。缩略图 `src` 取 `item.skuImg || item.productCoverImg`，无图显示占位。

### 4. 订单详情 `frontend/h5/src/views/OrderDetailPage.vue`

当前订单明细无图。在 `.item-row`（L48-61）左侧增加 56 px 缩略图，`src` 取 `item.skuImg`（快照值，可能为空串，空串时显示占位）。

> **不改动的部分**：`ProductCard.vue`（列表卡片保持商品主图）、`OrderListPage.vue`（继续使用 `summaryText` 文字摘要）、购物车算价接口（图片由 `CartItem` 经 pinia-plugin-persistedstate 本地持久化，不会丢失）。

## 五、部署与迁移

按以下顺序在生产环境上线。

1. 执行数据库迁移（仿 `migration_appointment_v1.sql` 头部注释的执行方式）：

   ```bash
   docker exec -i petorder-mysql mysql -uroot -proot123 petshop_order < backend/sql/migration_sku_img_v1.sql
   ```

2. 重建后端：`mvn package` 后 `docker compose build` 并 `up -d backend`。
3. 重建前端：在 `frontend/h5` 与 `frontend/admin` 分别执行 `pnpm build`，将 `dist` 部署到 nginx。
4. 整体走现有 git pull 部署流程（参考服务器部署记忆）。

## 六、验证

按以下场景端到端验证。

- **Admin 配图**：新建商品，为部分 SKU 上传图片、部分留空，保存后重新打开编辑弹窗，确认 `imgUrl` 回显正确；再次编辑进行换图或删图，保存后回显仍正确。
- **H5 弹窗切图**：打开 SKU 弹窗，默认展示第一个 SKU 的图片（无图时回退主图）；点击不同规格缩略图，顶部大图随之切换；缩略图本身也显示对应小图。
- **购物车**：加购不同 SKU，`CartDrawer` 每行显示对应 SKU 图片（无图回退主图）。
- **订单快照**：下单后订单详情 `OrderDetailPage` 每条明细显示 SKU 图片；随后在 Admin 修改该 SKU 的图片，**历史订单图片保持不变**，验证快照生效。
- **回归**：单 SKU 商品与服务弹窗正常；服务（SERVICE）SKU 配图正常；全程无图商品显示占位且不报错；订单列表 `summaryText` 不受影响。
- **构建检查**：`frontend/h5` 与 `frontend/admin` 的 `pnpm build` 通过 `vue-tsc` 类型检查；后端 `mvn package` 通过。

## 后续步骤

实施完成后，将本文档「状态」更新为「已实施完成并上线」，并在文末补记「实施差异」小节，记录与原方案的偏差，风格参考 `docs/appointment-system-plan.md`。

## 实施差异（2026-09-04）

实施时与原方案的实际偏差记录：

1. **弹窗形态经验收反馈调整（2026-09-04）**：方案原定「顶部大图 + 缩略图规格卡」，首版照此实施后验收反馈顶图过大。最终形态：**恢复原版「小图头部（76px）+ 文字价格」布局**，小图渲染当前 SKU 图（未配图回退主图），点击小图调用 Vant `showImagePreview` 看大图（占位态不可点，右下角附 `expand-o` 角标）；**规格缩略图卡保留**（56px，随 SKU 图切换）。切换 SKU 时小图与大图预览内容联动。
2. **DB 迁移未建脚本文件**：按需求方要求，未创建 `migration_sku_img_v1.sql`，改为直接进入 MySQL 容器交互执行两条 `ALTER TABLE`（本地 dev 已执行并验证）。生产部署前已补建脚本文件 `backend/sql/migration_sku_img_v1.sql` 供容器执行（见 `deploy/RELEASE-多店上线.md`）。`init.sql` 已同步更新。
3. **Admin 商品编辑弹窗加宽**：720px → 860px，以容纳「SKU图」列（约 80px），其余列宽未压缩。
4. **弹窗底部安全区**：未复用全局 `.safe-area-bottom`（避免与 scoped padding 优先级冲突），改在 `.sku-footer` 内直接写 `padding-bottom: calc(8px + env(safe-area-inset-bottom))`。
5. **验证结果**：`mvn package`、`frontend/h5` 与 `frontend/admin` 的 `pnpm build`（vue-tsc）全部通过；端到端场景（admin 配图/回显/换删图、H5 回退主图、切规格联动大图、全屏预览、加购缩略图、订单快照）在本地（默认店 main）全部验证通过。

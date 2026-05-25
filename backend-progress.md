# 后端开发进度

> 更新时间：2026-05-26

## 已完成

| Phase | 模块 | 提交 | 状态 |
|-------|------|------|------|
| 规范基线 | 冻结 modules.md / api.md 为唯一基准 | `5253796` | ✅ |
| Phase 0 | 项目骨架 + Docker MySQL + 基础框架类 | `4e04a4a` | ✅ |
| Phase 1 | 认证模块（C端短信 4 接口 + Admin 账密 3 接口） | `4e04a4a` `aacbab1` | ✅ |
| Phase 2 | 文件上传（图片上传/删除，本地存储） | `db879cd` | ✅ |
| Phase 3 | 商品与分类（分类 CRUD + 商品 SKU CRUD + C 端浏览） | `99aa48f` | ✅ |
| Phase 4 | 会员模块（等级 CRUD + 会员多手机号 + C 端身份匹配） | `2677064` | ✅ |
| Phase 5 | 价格计算引擎（GOODS 会员价 / SERVICE 折扣率，BigDecimal HALF_UP） | `55951cd` | ✅ |
| Phase 6 | 配送距离（Haversine + 起送价校验 + TIERED 分段运费） | `55951cd` | ✅ |
| Phase 7 | 系统配置（配置读写 + 分段运费 + Webhook SSRF 防护） | `5636d09` | ✅ |
| Phase 8 | 订单模块（创建/查询/快照/库存乐观锁扣减） | `44dc6d9` | ✅ |
| Phase 9 | 通知模块（企微 Webhook 异步推送） | `7bff103` | ✅ |
| Phase 10 | Admin 用户管理（CRUD + BOSS 保护 + BCrypt） | `dc0524d` | ✅ |
| Phase 11 | 操作日志（分页查询 + JOIN 用户名） | `52cf7c8` | ✅ |
| Phase 12 | 数据统计（总览/趋势/排行） | `f4f8eb9` | ✅ |

## 待开发

| Phase | 模块 | 说明 |
|-------|------|------|
| - | 短信验证码 | 当前验证码固定 `1234`，未对接真实短信服务商 |
| - | 订单通知 | 后端 Webhook 配置/测试已完成，下单时自动触发企微通知未实现 |
| Phase 13 | 集成联调 | 前后端联通测试（已完成） |
| Phase 14 | Docker 部署 | Spring Boot + Nginx + docker-compose |

## 下一步：本地跑起来

### 1. 启动 MySQL

```bash
cd backend
docker-compose up -d
```

验证建表：`docker exec -it petshop-mysql mysql -uroot -proot123 petshop_order -e "SHOW TABLES;"`

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

首次启动自动创建 BOSS 账号 `admin / admin123`，端口 8080。

### 3. 启动前端

```bash
cd frontend/h5 && pnpm dev      # 端口 3000
cd frontend/admin && pnpm dev    # 端口 3001
```

### 4. 全流程验证

| 操作 | 验证点 |
|------|--------|
| H5 验证码登录 | 固定验证码 `1234` |
| Admin `admin/admin123` 登录 | 端口 3001 |
| Admin 创建分类 + 商品 | 图片上传可用 |
| H5 浏览商品 | 会员/非会员 dealPrice 正确 |
| H5 加购 → 结算 → 下单 | 订单在 Admin 可见 |
| Admin 标记已处理 | 状态切换正常 |

## 技术栈

Spring Boot 3.3.6 / JDK 17 / MyBatis 3.0.4 / PageHelper 2.1.0 / Sa-Token 1.39.0 / MySQL 8.0(Docker) / HuTool 5.8.32

## 权威文档

`modules.md` > `api.md` > `backend-implementation-notes.md`

## 开发规划详细文件

`backend-planning/backend-dev-plan.md`

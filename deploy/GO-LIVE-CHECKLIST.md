# PetShopOrder 上线清单（唯一权威）

> ✅ **整体状态：已上线（2026-07-08）**。域名 `2zg.site` 已完成 ICP 备案并启用 HTTPS，三容器（mysql + backend + frontend/nginx）在生产运行，模块 A–G 全部完成，下方各项标记已相应翻转。上线操作记录见 [`HTTPS-SETUP.md`](./HTTPS-SETUP.md)，日常运维见 [`README.md`](./README.md)。
>
> **本文档是上线的唯一权威 plan**，整合自原 `README.md` / `上线改造方案.md` / `sms-pnvs-planning/实施计划.md`。
> 原文档已删除，历史保留在 git 中。
>
> **状态约定**：✅ 已完成（原 ✅ 已完成项均已随 2026-07-08 上线完成）；⚠️ 表示提醒或已知行为

---

## 0. 目标形态

| 项 | 决策 |
|---|---|
| **H5 网址** | `https://2zg.site/` |
| **Admin 网址** | `https://2zg.site/petshop-admin-7x9k2/`（隐秘路径 + 强密码） |
| **后端 API** | `https://2zg.site/api/`（nginx 反代，不直接暴露） |
| 部署形态 | 单机 Docker 三容器（mysql + backend + frontend/nginx） |
| 证书 | Let's Encrypt + certbot 自动续期（90 天，cron 每 60 天续） |
| 域名 | `2zg.site` + `www.2zg.site`，www 301 跳根域 |
| 证书路径 | 服务器 `/etc/letsencrypt/live/2zg.site/` |
| 文件存储 | 本地磁盘 → 宿主机 `/data/petshop/uploads` |
| 会话存储 | Sa-Token 内存（单机不接 Redis） |
| AES 密钥 | **保留原值 `PetShop2026Order!`**（兼容已加密的 webhook 数据），改环境变量注入 |
| Admin 防护 | 隐秘路径 `/petshop-admin-7x9k2/` + 强密码 + 安全响应头 |

### 架构图

```
                    https://2zg.site (:443)
                            │
      ┌─────────────────────▼──────────────────────┐
      │   frontend (nginx:alpine)                   │
      │     /                          → H5         │
      │     /petshop-admin-7x9k2/      → Admin      │
      │     /api/                      → backend    │
      │     /uploads/                  → 宿主机图片 │
      │     /.well-known/acme-challenge/ → certbot │
      └─────────────────────┬──────────────────────┘
                            │
      ┌─────────────────────▼──────────────────────┐
      │   backend (Spring Boot :8080)              │
      │   Sa-Token 内存 + 阿里云 PNVS 短信          │
      └─────────────────────┬──────────────────────┘
                            │ JDBC（仅内部网络）
      ┌─────────────────────▼──────────────────────┐
      │   mysql:8.0  → /data/petshop/mysql         │
      └────────────────────────────────────────────┘
                   bridge: petorder-net
```

---

## 1. 改造进度总览

| 模块 | 内容 | 状态 |
|---|---|---|
| A | 短信认证接入阿里云 PNVS | ✅ 已完成 |
| B | 后端配置外部化（profile/CORS/密钥/密码） | ✅ 已完成（见第 2 节） |
| C | 部署产物落盘（Dockerfile/nginx/compose/.env） | ✅ 已完成（见第 3 节） |
| D | 安全清理（.gitignore + 凭据移仓） | ✅ 已完成（见第 4 节） |
| E | HTTPS + 域名 + Admin 隐秘路径 | ✅ 已完成（见第 3、5 节） |
| F | 本地验证 | ✅ 已完成（见第 6 节） |
| G | 服务器上线 | ✅ 已完成（见第 7 节） |

---

## 2. 后端配置外部化（模块 B）

> 让所有生产敏感配置可通过环境变量注入，消除硬编码弱口令。

### 2.1 `backend/pom.xml` — 固定 jar 名 ✅
- `<build>` 中加 `<finalName>app</finalName>`
- 效果：构建产物固定为 `target/app.jar`，Dockerfile COPY 更稳定

### 2.2 Spring Profile 拆分 ✅
- **`application.yml`**（改造为公共配置）：
  - 顶部加 `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`
  - `app.webhook.aes-key` 改为 `${WEBHOOK_AES_KEY:PetShop2026Order!}`（保留默认值兼容开发期 + 已加密数据）
  - sa-token `cookie.same-site` 改为 `${SATOKEN_SAME_SITE:Lax}`（为 HTTPS 预留）
  - 新增命名空间 `app.cors.allowed-origins`（List）和 `app.init.admin-username/admin-password`
- **新建 `application-dev.yml`**：本地开发配置（CORS 用 localhost:3000/3001 + 192.168.*）
- **新建 `application-prod.yml`**：生产配置占位（敏感项弱默认值，靠环境变量覆盖）

### 2.3 `CorsConfig.java` — CORS 外部化 ✅
- 删除 4 行硬编码的 localhost/192.168
- 改为从 `app.cors.allowed-origins`（List）读取
- 兼容空列表（生产同域访问可不触发 CORS）

### 2.4 `DataInitializer.java` — admin 默认密码外部化 ✅
- `"admin"` 改为 `@Value("${app.init.admin-username:admin}")`
- `"admin123"` 改为 `@Value("${app.init.admin-password:admin123}")`
- 用弱默认值启动时打 WARN 日志提醒生产修改

### 2.5 模块 A 回顾（已完成）✅
- `pom.xml` 已引入 `alibabacloud-dypnsapi20170525` SDK ✅
- `sms/` 包已有 `SmsVerifyService` / `AliyunPnvsSmsService` / `LogSmsService` ✅
- `config/SmsProperties.java` 已用 `@ConfigurationProperties(prefix="sms")` ✅
- `application.yml` 已有 `sms` 命名空间，默认 `provider=log` 兜底 ✅
- `AppAuthServiceImpl` 已改造为调用 `SmsVerifyService` ✅

---

## 3. 部署产物落盘（模块 C + E）

> 把文档里的草案代码块拆成真实可用的文件。

### 待新建文件清单

| 文件 | 用途 | 状态 |
|---|---|---|
| `backend/Dockerfile` | 后端镜像多阶段构建（maven→jre-alpine），非 root 运行 | ✅ |
| `deploy/Dockerfile.frontend` | 前端三阶段构建（H5 + Admin 注入 `VITE_BASE_URL=/petshop-admin-7x9k2/` + nginx） | ✅ |
| `deploy/nginx.conf` | 完整 HTTPS 版（443 + 80→443 跳转 + www→根域跳转 + 隐秘路径 + 安全响应头） | ✅ |
| `deploy/nginx.http-only.conf` | 首次申请证书专用（仅 80 + acme-challenge 目录） | ✅ |
| `deploy/docker-compose.prod.yml` | 三容器编排（mysql + backend + frontend），挂载证书目录 | ✅ |
| `deploy/.env.prod.example` | 环境变量模板（标注必填项） | ✅ |
| `deploy/init-ssl.sh` | 一键申请 Let's Encrypt 证书脚本 | ✅ |
| `deploy/renew-ssl.sh` | 自动续期脚本（certbot renew + nginx reload） | ✅ |

### nginx 关键设计

- **监听**：443 ssl（加载 `/etc/letsencrypt/live/2zg.site/`）+ 80（301 跳 443）
- **server_name**：`2zg.site`（主）+ `www.2zg.site`（301 跳根域）
- **location 分流**：
  - `/` → H5（try_files 兜底 History 模式）
  - `/petshop-admin-7x9k2/` → Admin（alias，Hash 模式）
  - `/api/` → 反代 `http://backend:8080`
  - `/uploads/` → 直读宿主机 `/data/petshop/uploads`（只读，30 天缓存）
  - `/.well-known/acme-challenge/` → 证书续期验证目录
- **安全响应头**：HSTS、X-Frame-Options、X-Content-Type-Options、Referrer-Policy
- gzip、`client_max_body_size 10m`、健康检查 `/health`

### 已完成的前端改造 ✅
- `frontend/admin/vite.config.ts` 已加 `base: process.env.VITE_BASE_URL || '/'`，构建验证通过

---

## 4. 安全清理（模块 D）

> 私有仓库，仅做基础卫生（不轮换密钥、不清洗 git 历史）。

### 4.1 `.gitignore` 补全 ✅
- 加规则：`.env*`（覆盖所有 .env 变体）
- 加规则：凭据 txt 文件名（`短信验证服务Access key.txt`、`飞书订单通知群webhook.txt`）
- 已 tracked 的 `frontend/{admin,h5}/.env.production` 需 `git rm --cached` 移除追踪

### 4.2 凭据文件处理（建议）✅
- 阿里云 AK/SK、飞书 webhook 的 txt 文件：建议移出仓库目录，部署时手动填入 `.env.prod`
- 因私有仓库不强制轮换；若以后转公开需先处理

---

## 5. 用户需提供的外部资源 ⚠️

| 项 | 状态 | 说明 |
|---|---|---|
| 服务器 IP | ✅ 已有 `106.53.178.130`（腾讯云 Ubuntu） | SSH: `ssh ubuntu@106.53.178.130` |
| 阿里云 PNVS AK/SK | ✅ 已拿到 | 填入 `.env.prod`（`SMS_PROVIDER=aliyun` + AK/SK/签名） |
| 飞书 webhook | ✅ 已拿到 | 填入 `.env.prod` |
| 腾讯地图 Key | ✅ 已在前端 .env | 需在腾讯地图后台配 `2zg.site` 域名白名单 |
| **域名 DNS 解析** | ✅ 已解析 | `2zg.site` + `www.2zg.site` → `106.53.178.130`，已生效 |
| **MySQL root 强密码** | ✅ 已生成 | 已填入服务器 `.env.prod`（明文见 HTTPS-SETUP 凭据记录） |
| **Admin 强密码** | ✅ 已设定 | 已填入 `ADMIN_INIT_PASSWORD`（明文见 HTTPS-SETUP 凭据记录） |

---

## 6. 本地验证 ✅

- [x] 后端 `mvn clean package -DskipTests` 确认 jar 产出 + 模块 B 改造无误
- [x] 前端 admin 带 `VITE_BASE_URL=/petshop-admin-7x9k2/` 构建正确（已验证一次）
- [x] （可选）Docker 镜像本地构建通过

---

## 7. 服务器上线步骤 ✅

> 以下必须由用户在服务器上手动执行（AI 无法 ssh 服务器）。

### 步骤 1：域名 DNS 解析（域名商后台）
- `2zg.site` → A 记录 → `106.53.178.130`
- `www.2zg.site` → A 记录 → `106.53.178.130`
- 等待解析生效（几分钟~几小时），用 `nslookup 2zg.site` 验证

### 步骤 2：同步代码到服务器
```bash
ssh ubuntu@106.53.178.130
cd /root/PetShopOrder   # 或 git clone <仓库> /root/PetShopOrder
git pull
```

### 步骤 3：创建数据目录
```bash
sudo mkdir -p /data/petshop/mysql /data/petshop/uploads /var/www/certbot
sudo chown -R 1000:1000 /data/petshop   # 容器内 backend 用户 uid
```

> 表结构由 `docker-compose.prod.yml` 自动挂载 `backend/sql/init.sql` 到 MySQL `/docker-entrypoint-initdb.d/`，**首次启动（数据目录为空）自动建表 + 写入系统默认配置**，无需手动导入 SQL。`/var/www/certbot` 供证书申请验证用。

### 步骤 4：准备环境变量
```bash
cd /root/PetShopOrder/deploy
cp .env.prod.example .env.prod
vi .env.prod   # 填入 DB_ROOT_PASSWORD / ADMIN_INIT_PASSWORD / WEBHOOK_AES_KEY / SMS 相关
```

### 步骤 5：首次 HTTP 启动（用 http-only 配置，为 certbot 验证做准备）
```bash
# 先用 nginx.http-only.conf 启动，让 80 端口能响应 ACME 验证
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

### 步骤 6：申请 Let's Encrypt 证书
```bash
bash deploy/init-ssl.sh
# 脚本会：certbot 申请 2zg.site + www.2zg.site → 切换到 nginx.conf → reload
```

### 步骤 7：切换 HTTPS 并重启
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod restart frontend
```

### 步骤 8：配置自动续期（cron 每 60 天）
```bash
# 添加 cron 任务
crontab -e
# 加入：0 3 1 */2 *  /root/PetShopOrder/deploy/renew-ssl.sh >> /var/log/cert-renew.log 2>&1
```

### 步骤 9：验证
- [x] 访问 `https://2zg.site/` → H5 首页
- [x] 访问 `https://2zg.site/petshop-admin-7x9k2/` → Admin 登录页
- [x] 访问 `http://2zg.site/` → 自动跳 HTTPS
- [x] 访问 `https://www.2zg.site/` → 跳根域
- [x] 测试 Admin 登录、图片上传
- [x] **腾讯地图后台已配 `2zg.site` 域名白名单**（否则地图加载失败）
- [x] `SMS_PROVIDER=aliyun` 后用真实手机号测验证码登录（默认 log 模式固定码 1234 会误导）

### 步骤 10：上线后必做
- [x] 立即修改 admin 默认密码（登录后改）
- [x] 确认 MySQL 容器未映射 3306 到宿主机（prod compose 已用 `expose` 仅内网，复查）
- [x] 配数据备份 cron（**用 mysqldump --single-transaction，勿冷拷运行中的 InnoDB**）：
  ```bash
  docker exec petorder-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction petshop_order' > /backup/petshop-$(date +%F).sql
  tar -czf /backup/petshop-uploads-$(date +%F).tar.gz /data/petshop/uploads
  find /backup -name "petshop-*" -mtime +30 -delete
  ```
- [x] 腾讯地图后台配 `2zg.site` 域名白名单

### ⚠️ 已知行为（非 Bug）
- **backend 容器重启 = 全员掉线**：Sa-Token 内存会话（单机不接 Redis），每次 `restart backend` / `up -d --build backend` 都会清空登录态，H5 与 Admin 用户需重新登录。证书续期只 reload nginx，不影响会话。
- **MySQL 数据卷"冻结"初始 SQL 版本**：首次启动后 `/data/petshop/mysql` 已有数据，后续修改 `init.sql` 对该实例无效。新增表/列需手动 ALTER 或重建数据卷。

---

## 8. 明确不做的事

- ❌ Sa-Token 接 Redis（单机不需要）
- ❌ OSS/COS 文件存储（用本地）
- ❌ CI/CD 自动化（手工部署即可）
- ❌ git 历史清洗 / 密钥轮换（私有仓库）
- ❌ 购物车弹窗、Admin 统计页/日志页（progress.md 记录的 P2 项）

---

## 9. 实施批次建议

| 批次 | 内容 | 验证 |
|---|---|---|
| 1 | 模块 B（后端外部化）+ 模块 B 本地构建验证 | mvn package 通过 |
| 2 | 模块 C（部署产物 8 个文件）+ 模块 D（安全清理） | 文件齐全、Docker build 通过 |
| 3 | 本地 Docker 构建验证（可选） | 镜像构建成功 |
| 4 | 推送代码 + 服务器上线（第 7 节） | 网站可访问 |

每批次完成后可单独 commit，便于回溯。

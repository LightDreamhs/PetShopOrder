# PetShopOrder 部署运维手册

> **上线 plan 与待办清单**：见 [`GO-LIVE-CHECKLIST.md`](./GO-LIVE-CHECKLIST.md)（唯一权威）
>
> **本文档定位**：部署完成后的**日常运维参考**，包含架构说明、数据目录、日志查看、服务更新、数据备份等。部署产物文件（Dockerfile / nginx.conf / docker-compose.prod.yml 等）以仓库内的实际文件为准，不再在本文档内维护代码副本。

---

## 1. 部署形态

单机 Docker 三容器编排，前端 H5 与 Admin 共用一个 nginx 容器，业务数据（MySQL、图片）以宿主机目录持久化。

```
                    https://2zg.site (:443)
                            │
      ┌─────────────────────▼──────────────────────┐
      │   frontend (nginx:alpine)                   │
      │     /                          → H5         │
      │     /petshop-admin-7x9k2/      → Admin      │
      │     /api/                      → backend    │
      │     /uploads/                  → 宿主机图片 │
      └─────────────────────┬──────────────────────┘
                            │
      ┌─────────────────────▼──────────────────────┐
      │   backend (Spring Boot :8080)              │
      │   Sa-Token 内存 + 阿里云 PNVS 短信          │
      │   上传写入 → /app/uploads → 宿主机         │
      └─────────────────────┬──────────────────────┘
                            │ JDBC（仅内部网络，3306 不暴露公网）
      ┌─────────────────────▼──────────────────────┐
      │   mysql:8.0  → /data/petshop/mysql         │
      └────────────────────────────────────────────┘
                   bridge: petorder-net
```

| 项 | 决策 |
|----|------|
| 容器数量 | 3 个（`frontend` / `backend` / `mysql`） |
| 前端 | 一个 nginx 容器托管 H5 与 Admin，按路径分流 |
| 后端端口 | `8080` 仅容器内可达，不映射到宿主机 |
| MySQL 端口 | **不暴露**，仅容器内网络通信 |
| 图片存储 | 宿主机 `/data/petshop/uploads`，nginx 直读、后端写入，H5 与 Admin 共享同一份 |
| MySQL 数据 | 宿主机 `/data/petshop/mysql`（bind mount） |
| 配置注入 | 环境变量（`.env.prod`），敏感值不进镜像 |
| 证书 | Let's Encrypt（certbot 自动续期） |
| 认证 | Sa-Token 内存会话 + 阿里云号码认证服务（PNVS）短信验证 |

---

## 2. 服务器目录与数据规划

| 宿主机路径 | 用途 | 挂载方式 |
|-----------|------|---------|
| `/data/petshop/mysql` | MySQL 数据 | mysql 容器读写 |
| `/data/petshop/uploads` | 上传图片（H5 与 Admin 共享） | backend 读写、frontend 只读 |
| `/etc/letsencrypt` | Let's Encrypt 证书 | frontend 容器只读挂载 |
| `/home/ubuntu/PetShopOrder` | 代码仓库与部署配置 | git 同步 |

---

## 3. 图片存储方案

图片采用"后端写入、nginx 直读、两端共享"的模式，后端不参与图片读取请求。

- **写入**：`FileServiceImpl` 将文件写入 `${UPLOAD_DIR}`，生产环境通过 `UPLOAD_DIR=/app/uploads` 指向挂载目录，对应宿主机 `/data/petshop/uploads`。
- **读取**：nginx 的 `location /uploads/` 直接读取宿主机 `/data/petshop/uploads`，请求不会到达后端容器。
- **URL 一致性**：后端返回的图片 URL 形如 `/uploads/<key>`，与 nginx 暴露的路径一致，H5 与 Admin 访问同一地址即可获得同一份图片。
- **后端静态映射**：`WebMvcConfig` 中现有的 `/uploads/**` 资源映射在生产可保留（nginx 已先行拦截，不会触发）。

---

## 4. 常用运维命令

> 以下命令在服务器 `/home/ubuntu/PetShopOrder/deploy` 目录下执行，需先准备好 `.env.prod`。

### 查看服务状态
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
```

### 查看日志
```bash
# 实时跟随后端日志
docker compose -f docker-compose.prod.yml logs -f backend

# 查看 nginx 访问日志
docker compose -f docker-compose.prod.yml logs -f frontend
```

### 健康检查
```bash
curl https://2zg.site/health
```

### 更新 backend（本地构建 jar → 上传 → 服务器组装运行镜像）

> 后端 Dockerfile 的 Maven 阶段要在容器内从 Maven Central 全量拉依赖：服务器直连实测 ~126 KB/s，
> 冷启动 1 小时起步，2C2G 下编译还易 OOM；且该流程依赖 Docker 层缓存（pom 未变则跳过拉依赖），
> 缓存一旦被 prune 清掉就必然回到冷启动（2026-09-26 实测卡死 30 分钟+，CPU 空转纯等网络）。
> 因此 backend 与 frontend 同策略（2026-09-26 拍板）：**本地构建，服务器只组装运行镜像**。

```bash
bash deploy/deploy-backend.sh
```

等价手工流程：

```bash
# 1) 本地打包
cd backend && mvn -q clean package -DskipTests && cd ..
# 2) 上传 jar 与运行时 Dockerfile
scp backend/target/app.jar ubuntu@106.53.178.130:~/PetShopOrder/backend/app-deploy.jar
scp backend/Dockerfile.runtime ubuntu@106.53.178.130:~/PetShopOrder/backend/Dockerfile.runtime
# 3) 服务器组装运行镜像（秒级，运行阶段配置与 Dockerfile 完全一致）
ssh ubuntu@106.53.178.130 "cd ~/PetShopOrder/backend && docker build -f Dockerfile.runtime -t deploy-backend:latest ."
# 4) 重建容器（--no-deps/--no-build 防 compose 连带构建其它服务）
ssh ubuntu@106.53.178.130 "cd ~/PetShopOrder/deploy && docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --no-deps --no-build backend"
# 5) 健康检查
ssh ubuntu@106.53.178.130 "docker inspect --format '{{.State.Health.Status}}' petorder-backend"
```

> ⚠️ `backend/Dockerfile.runtime` 与 `backend/Dockerfile` 的运行阶段必须逐行一致
> （JVM 参数、healthcheck、时区、非 root 用户），改其中一份必须同步另一份。

#### 应急：服务器端构建 backend（本地机器不可用时，慎用）

```bash
ssh ubuntu@106.53.178.130 "cd ~/PetShopOrder && git pull && cd deploy && docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build backend"
```

> 仅限应急：Maven Central 直连限速，层缓存冷启动后 1 小时起步。
> 确需走此路径，先给 `backend/Dockerfile` 配阿里云 Maven 镜像源（settings.xml mirror）再执行。

### 更新前端（H5 / Admin：本地构建 → 上传产物 → 重建容器）

> 服务器 2C2G 跑不动 node 构建（2026-09-23 拍板）：前端一律**本地构建**，
> `Dockerfile.frontend` 只 COPY 产物（`.dockerignore` 已放行 `frontend/{h5,admin}/dist`）。
> 注意：服务器上 `git pull` 只更新代码，**前端产物必须按下面步骤单独上传**。
>
> ⚠️ admin 的 base 写死在 `frontend/admin/vite.config.ts`（`/petshop-admin-7x9k2/`），
> **禁止**再用 `VITE_BASE_URL=...` 环境变量或 `--base /...` 命令行参数注入：
> Git Bash（MSYS）会把以 `/` 开头的值自动转换成 `C:/Program Files/Git/...`，污染构建产物导致白屏
> （2026-09-26 白屏事故根因）。admin 构建末尾会自动跑 `scripts/check-dist.mjs` 拦截坏产物。

```bash
# 1) 本地构建（Git Bash，项目根目录；admin 构建末尾自动执行产物自检）
cd frontend/h5 && pnpm build && cd ../admin && pnpm build && cd ../..

# 2) 上传产物（dist/. 写法保证覆盖内容而不嵌套目录）
#    admin：先清空远端目录再传，避免旧 hash 文件残留
#    H5：⚠️ 只做覆盖上传、禁止清空重建——线上 H5 产物可能含站外机器的修复，
#        上传前先比对 md5：本地 md5sum frontend/h5/dist/index.html vs 服务器同路径
ssh ubuntu@106.53.178.130 "rm -rf ~/PetShopOrder/frontend/admin/dist && mkdir -p ~/PetShopOrder/frontend/admin/dist ~/PetShopOrder/frontend/h5/dist"
scp -r frontend/h5/dist/.  ubuntu@106.53.178.130:~/PetShopOrder/frontend/h5/dist/
scp -r frontend/admin/dist/. ubuntu@106.53.178.130:~/PetShopOrder/frontend/admin/dist/

# 3) 服务器重建 frontend 容器（纯 COPY，秒级）
#    ⚠️ --no-deps 必须：compose 的 up --build 会把 depends_on 的 backend 拉进构建集，
#    在服务器上跑完整 Maven 构建（慢、易 OOM，2026-09-26 实测踩坑）
ssh ubuntu@106.53.178.130 "cd ~/PetShopOrder/deploy && docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build --no-deps frontend"
```

> ⚠️ `Dockerfile.frontend` 不要加 `# syntax=docker/dockerfile:1` 之类的 BuildKit 声明：
> 服务器访问 Docker Hub 被墙、腾讯镜像源不含 BuildKit frontend 镜像，加了之后
> `--build` 会永久卡死（2026-09-26 实测并已移除）。正常情况 `--build` 应秒级完成。

#### 应急：服务器端构建 admin（本地机器不可用时）

服务器 2C2G + swap 4G 可跑通单个前端项目构建（2026-09-26 实测），用 node 容器构建、不污染宿主机：

```bash
ssh ubuntu@106.53.178.130 "cd ~/PetShopOrder && git pull && bash deploy/emergency-build-admin.sh"
# 构建成功后重建容器（命令同上第 3 步）
```

H5 没有应急构建脚本：见上方「H5 产物以服务器为准」警告，勿在服务器上重建 H5。

### 重启服务
```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod restart backend
```

---

## 5. 数据备份

建议加入 cron 每天执行：

```bash
# 打包 MySQL 数据与上传图片到 /backup
sudo tar -czf /backup/petshop-$(date +%F).tar.gz /data/petshop/mysql /data/petshop/uploads

# 保留最近 30 天的备份
sudo find /backup -name "petshop-*.tar.gz" -mtime +30 -delete
```

cron 配置（每天凌晨 3 点）：
```
0 3 * * * /usr/local/bin/petshop-backup.sh >> /var/log/petshop-backup.log 2>&1
```

---

## 6. 证书续期

Let's Encrypt 证书有效期 90 天，已配置 cron 每 60 天自动续期（见 `renew-ssl.sh`）。

手动测试续期：
```bash
docker run --rm -v /etc/letsencrypt:/etc/letsencrypt certbot/certbot renew --dry-run
```

手动续期 + reload nginx：
```bash
bash /home/ubuntu/PetShopOrder/deploy/renew-ssl.sh
```

---

## 7. 后续演进

- 视访问量增长评估是否引入 Redis 承接 Sa-Token 会话
- 视图片数量增长评估是否迁移到 OSS/COS
- 视团队规模评估是否引入 CI/CD 自动化部署

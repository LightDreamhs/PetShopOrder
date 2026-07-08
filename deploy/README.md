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

### 更新服务（以 backend 为例）
```bash
cd /home/ubuntu/PetShopOrder
git pull
cd deploy
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build backend
```

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

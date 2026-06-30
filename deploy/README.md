# PetShopOrder 生产部署文档

本文档描述 PetShopOrder 项目的目标生产部署形态，覆盖架构、部署产物（Dockerfile / `docker-compose.prod.yml` / `nginx.conf` / `.env.prod`）、部署步骤与上线检查项。**文档定位为"部署操作手册"，落地方案见 [`上线改造方案.md`](./上线改造方案.md)。当前状态：方案已拍板，尚未实施。**

适用对象：负责部署与运维的开发人员。项目为单体架构、日访问量与 QPS 较低，采用单机 Docker 部署即可满足需求。

---

## 1. 目标部署形态

整体为单机 Docker 三容器编排，前端 H5 与 Admin 共用一个 nginx 容器，业务数据（MySQL、图片）以宿主机目录持久化。

```
                       http://<服务器 IP> (:80)
                                │
          ┌─────────────────────▼──────────────────────┐
          │   frontend (1 个容器)  nginx:alpine         │
          │     /         → H5 静态 (History 模式)      │
          │     /admin/   → Admin 静态 (Hash 模式)      │
          │     /api/     → 反代 backend:8080           │
          │     /uploads/ → 直读宿主机图片 (只读)  ─────┼─→ /data/petshop/uploads
          └─────────────────────┬──────────────────────┘
                                │
          ┌─────────────────────▼──────────────────────┐
          │   backend (1 个容器)  Spring Boot :8080     │
          │   Sa-Token 内存会话 + 阿里云短信认证        │
          │   上传写入 → /app/uploads (读写)  ─────────┼─→ /data/petshop/uploads
          └─────────────────────┬──────────────────────┘
                                │ JDBC（仅内部网络，3306 不暴露公网）
          ┌─────────────────────▼──────────────────────┐
          │   mysql (1 个容器)  mysql:8.0               │
          │   数据持久化 → /data/petshop/mysql          │
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
| 认证 | Sa-Token 内存会话 + 阿里云号码认证服务（PNVS）短信验证 |

---

## 2. 前置代码改造

部署依赖若干代码改造，部分当前尚未完成。下表标注每项的状态与必要性。

| 改造项 | 必要性 | 当前状态 | 说明 |
|--------|--------|----------|------|
| Admin 子路径资源引用 | **必需** | 未实施 | Admin 挂在 `/admin/`，vite `base` 必须为 `/admin/`，否则静态资源 404。详见 [3.2](#32-前端-dockerfile) 与 [6](#6-图片存储方案) |
| 数据库与上传目录外部化 | 已就绪 | 已支持 | `application.yml` 已用 `${DB_HOST}` / `${UPLOAD_DIR}` 等占位符 |
| Spring Profile 机制 | 建议 | 未实施 | 当前为单一 `application.yml`，建议拆分 `application-dev.yml` / `application-prod.yml` |
| AES 密钥外部化 | 建议 | 未实施 | `app.webhook.aes-key` 当前硬编码，需改为 `${WEBHOOK_AES_KEY}` |
| CORS 白名单外部化 | 建议 | 未实施 | 生产同域访问可不触发 CORS，跨域场景需外部化 |
| Admin 初始密码外部化 | 建议 | 未实施 | `DataInitializer` 的 `admin/admin123` 建议改环境变量 |
| 阿里云短信接入 | 可选 | 未实施 | 不影响部署形态，运行时功能。`pom.xml` 尚未引入 PNVS SDK |

> **提示**：上述"必需"项未完成前，不建议执行部署。"建议"项缺失不影响启动，但上线前应补齐。

---

## 3. 部署产物草案

以下配置为目标态草案。落地实施时，将其拆分为独立文件放入 `deploy/` 与各模块根目录。产物路径约定：

- `backend/Dockerfile`
- `deploy/Dockerfile.frontend`
- `deploy/nginx.conf`
- `deploy/docker-compose.prod.yml`
- `deploy/.env.prod`

### 3.1 后端 Dockerfile

多阶段构建，产物为 `petshop-order-*.jar`。构建上下文为 `backend/`。

```dockerfile
# backend/Dockerfile
# 阶段 1：构建
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# 阶段 2：运行
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /app/target/petshop-order-*.jar app.jar
RUN mkdir -p /app/uploads && chown -R spring:spring /app
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3.2 前端 Dockerfile

一个 Dockerfile 同时构建 H5 与 Admin，输出到同一个 nginx 镜像。构建上下文为项目根目录。前端使用 Vue 3 + Vite 8 + pnpm，Vite 8 要求 Node 20.19 或 22.12 及以上，故使用 `node:22-alpine`。

```dockerfile
# deploy/Dockerfile.frontend
# 阶段 1：构建 H5（根路径，base 默认 /）
FROM node:22-alpine AS h5-builder
RUN npm install -g pnpm
WORKDIR /h5
COPY frontend/h5/package.json frontend/h5/pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile || pnpm install
COPY frontend/h5/ .
RUN pnpm build

# 阶段 2：构建 Admin（子路径，base=/admin/）
FROM node:22-alpine AS admin-builder
RUN npm install -g pnpm
WORKDIR /admin
COPY frontend/admin/package.json frontend/admin/pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile || pnpm install
COPY frontend/admin/ .
ARG VITE_BASE_URL=/admin/
ENV VITE_BASE_URL=${VITE_BASE_URL}
RUN pnpm build

# 阶段 3：运行（同一 nginx 托管两套产物）
FROM nginx:alpine
RUN apk add --no-cache wget
COPY --from=h5-builder    /h5/dist    /usr/share/nginx/html/h5
COPY --from=admin-builder /admin/dist /usr/share/nginx/html/admin
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

配套改造：`frontend/admin/vite.config.ts` 需将 `base` 参数化，使其读取构建期注入的 `VITE_BASE_URL`。

```ts
// frontend/admin/vite.config.ts 顶部配置中新增 base
export default defineConfig({
  base: process.env.VITE_BASE_URL || '/',   // 新增：默认根路径，构建时注入 /admin/
  // ...其余保持不变
})
```

> **警告**：若 Admin 的 `base` 保持默认 `/`，部署到 `/admin/` 后所有 JS 与 CSS 会请求 `/assets/...`，被 `location /`（H5）拦截，导致 Admin 页面白屏。此项为部署前必需改造。

### 3.3 nginx.conf

```nginx
# deploy/nginx.conf
server {
    listen 80;
    server_name _;
    client_max_body_size 10m;

    gzip on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/javascript application/xml;

    # H5（根路径，History 模式需 try_files 兜底）
    location / {
        root /usr/share/nginx/html/h5;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # Admin（/admin/ 子路径，Hash 模式）
    location /admin/ {
        alias /usr/share/nginx/html/admin/;
        try_files $uri $uri/ /admin/index.html;
    }

    # 后端 API 反向代理
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 图片：nginx 直读宿主机，H5 与 Admin 共享（不走后端容器）
    location /uploads/ {
        alias /data/petshop/uploads/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # 健康检查
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
```

### 3.4 docker-compose.prod.yml

```yaml
# deploy/docker-compose.prod.yml
services:
  mysql:
    image: mysql:8.0
    container_name: petorder-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: ${DB_NAME:-petshop_order}
      TZ: Asia/Shanghai
    # 注意：不映射 ports，3306 仅容器内可达
    volumes:
      - /data/petshop/mysql:/var/lib/mysql
      - ../backend/sql/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --default-time-zone=+08:00
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s
    networks: [petorder-net]

  backend:
    build:
      context: ../backend
      dockerfile: Dockerfile
    container_name: petorder-backend
    restart: unless-stopped
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: ${DB_NAME:-petshop_order}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      UPLOAD_DIR: /app/uploads
      SPRING_PROFILES_ACTIVE: prod
      # 以下需对应代码改造完成后生效：
      # WEBHOOK_AES_KEY: ${WEBHOOK_AES_KEY}
      # SMS_PROVIDER: ${SMS_PROVIDER:-log}
      # ALIYUN_SMS_AK: ${ALIYUN_SMS_AK}
      # ALIYUN_SMS_SK: ${ALIYUN_SMS_SK}
      # ALIYUN_SMS_SIGN: ${ALIYUN_SMS_SIGN}
      # ALIYUN_SMS_TEMPLATE: ${ALIYUN_SMS_TEMPLATE:-100001}
    volumes:
      - /data/petshop/uploads:/app/uploads
    depends_on:
      mysql:
        condition: service_healthy
    networks: [petorder-net]

  frontend:
    build:
      context: ..
      dockerfile: deploy/Dockerfile.frontend
    container_name: petorder-frontend
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - /data/petshop/uploads:/data/petshop/uploads:ro
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - backend
    networks: [petorder-net]

networks:
  petorder-net:
    driver: bridge
```

### 3.5 .env.prod 清单

复制为 `.env.prod` 后填入真实值。标注"已支持"的变量当前代码即可读取，其余需对应代码改造。

```dotenv
# deploy/.env.prod.example
# ===== MySQL =====
DB_ROOT_PASSWORD=请改为强随机密码
DB_NAME=petshop_order
DB_USER=root
DB_PASSWORD=同 DB_ROOT_PASSWORD

# ===== 后端 =====
SPRING_PROFILES_ACTIVE=prod              # 需 Profile 改造（建议）
# WEBHOOK_AES_KEY=请改为强随机值         # 需 AES 密钥外部化（建议）

# ===== 阿里云短信（模块 A 完成后启用） =====
SMS_PROVIDER=log                         # log 兜底 / aliyun
ALIYUN_SMS_AK=
ALIYUN_SMS_SK=
ALIYUN_SMS_SIGN=
ALIYUN_SMS_TEMPLATE=100001

# ===== 其他（需对应改造） =====
# CORS_ALLOWED_ORIGINS=
# ADMIN_INIT_PASSWORD=
```

---

## 4. 服务器目录与数据规划

| 宿主机路径 | 用途 | 挂载方式 |
|-----------|------|---------|
| `/data/petshop/mysql` | MySQL 数据 | mysql 容器读写 |
| `/data/petshop/uploads` | 上传图片（H5 与 Admin 共享） | backend 读写、frontend 只读 |
| `/root/PetShopOrder/deploy` | 部署配置与脚本 | 代码仓库 |

> **提示**：首次部署前手动创建上述目录并设置权限：`mkdir -p /data/petshop/mysql /data/petshop/uploads`。

---

## 5. 部署步骤

1. 将代码同步到服务器 `/root/PetShopOrder`。
2. 创建数据目录：`mkdir -p /data/petshop/mysql /data/petshop/uploads`。
3. 进入部署目录：`cd /root/PetShopOrder/deploy`。
4. 准备环境变量：`cp .env.prod.example .env.prod`，填入真实密码与密钥。
5. 启动服务：

   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
   ```

6. 验证服务状态：

   ```bash
   docker compose -f docker-compose.prod.yml ps
   curl http://localhost/health
   ```

---

## 6. 图片存储方案

图片采用"后端写入、nginx 直读、两端共享"的模式，后端不参与图片读取请求。

- **写入**：`FileServiceImpl` 将文件写入 `${UPLOAD_DIR}`，生产环境通过 `UPLOAD_DIR=/app/uploads` 指向挂载目录，对应宿主机 `/data/petshop/uploads`。
- **读取**：nginx 的 `location /uploads/` 直接读取宿主机 `/data/petshop/uploads`，请求不会到达后端容器。
- **URL 一致性**：后端返回的图片 URL 形如 `/uploads/<key>`，与 nginx 暴露的路径一致，H5 与 Admin 访问同一地址即可获得同一份图片。
- **后端静态映射**：`WebMvcConfig` 中现有的 `/uploads/**` 资源映射在生产可保留（nginx 已先行拦截，不会触发），也可在确认 nginx 方案后移除。

---

## 7. 上线检查清单

- [ ] Admin 的 vite `base` 已参数化为 `/admin/` 并验证资源加载正常。
- [ ] `.env.prod` 中密码、AES 密钥均为强随机值，且文件权限受限。
- [ ] MySQL 容器未映射 `3306` 到宿主机（仅内部网络）。
- [ ] 默认 `admin/admin123` 初始密码已修改。
- [ ] 阿里云号码认证服务已开通、AK 已配置（或确认使用 `log` 兜底）。
- [ ] 首次部署后 MySQL 初始化脚本执行成功，核心表结构就绪。
- [ ] H5 首页与 Admin 控制台均可正常打开，登录与图片上传功能验证通过。

---

## 8. 运维

**查看日志**：

```bash
docker compose -f docker-compose.prod.yml logs -f backend
```

**更新服务**（以 backend 为例）：

```bash
cd /root/PetShopOrder
git pull
cd deploy
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build backend
```

**数据备份**（建议加入 cron）：

```bash
# 打包 MySQL 与图片
tar -czf /backup/petshop-$(date +%F).tar.gz /data/petshop/mysql /data/petshop/uploads
```

---

## 9. 后续步骤

- 完成第 2 节中标注"必需"与"建议"的代码改造。
- 域名到位后，为 nginx 配置 HTTPS 并将 Sa-Token 的 `same-site` 切换为 `None; Secure`。
- 视访问量增长评估是否引入 Redis 承接 Sa-Token 会话。

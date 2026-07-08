# HTTPS 上线操作手册

> **上线总清单**：见 [`GO-LIVE-CHECKLIST.md`](./GO-LIVE-CHECKLIST.md)（唯一权威）。
>
> **本文档定位**：`2zg.site` ICP 备案通过后，执行 HTTPS 上线的操作手册。每一步都带验证命令，按顺序做完即可启用 HTTPS 与证书自动续期。步骤全部完成后，回到本手册顶部勾选确认。

本手册覆盖：验证 80 端口解封、申请 Let's Encrypt 证书、切换 nginx 到 HTTPS、配置证书自动续期与数据备份。备案期间腾讯云对未备案域名的 80 端口做了 webblock 阻断，会导致 Let's Encrypt HTTP-01 验证失败；备案通过后 80 解封，证书即可正常申请。

---

## 前置条件

开始前逐项确认，全部满足再执行后续步骤：

- [ ] ICP 备案已通过，腾讯云已解除 `2zg.site` 的 80 端口 webblock 阻断。
- [ ] `2zg.site` 与 `www.2zg.site` 的 DNS A 记录都已指向 `106.53.178.130`（证书同时覆盖主域和 www，两者 DNS 必须都到位）。
- [ ] 腾讯云安全组已放行入站 **80 与 443**（80 备案解封后放行；443 必须单独确认放开，否则证书申请成功也访问不通 HTTPS）。
- [ ] 服务器三容器（`petorder-mysql` / `petorder-backend` / `petorder-frontend`）在运行。
- [ ] `deploy/.env.prod` 已填好敏感值（`DB_ROOT_PASSWORD` / `ADMIN_INIT_PASSWORD`）。
- [ ] `frontend` 容器用的是含 ACME 路由的 80 配置：`nginx.conf.ip`（备案期间 IP 访问，推荐）或 `nginx.http-only.conf`（占位页）。两者都保留了 `/.well-known/acme-challenge/` 路由，可直接申请证书（`.env.prod` 的 `NGINX_CONF` 对应 `./nginx.conf.ip` 或留空）。

> **提示**：若服务器刚开机或容器未运行，先恢复：
>
> ```bash
> ssh ubuntu@106.53.178.130 "cd /home/ubuntu/PetShopOrder && docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d"
> ```

---

## 上线步骤

### 1. 验证 80 端口已解封

备案通过后，腾讯云会解除 80 端口阻断。确认 80 能正常响应（不是备案提示页）：

```bash
curl -i http://2zg.site/
```

- ✅ 返回 200（`nginx.conf.ip` 下是 H5 首页 HTML，`nginx.http-only.conf` 下是 `petshop cert init: waiting for ssl`）：80 已解封，继续下一步。
- ❌ 仍跳转 `dnspod.qcloud.com/static/webblock.html`：阻断未解除，等待腾讯云生效（通常备案通过后数小时内），或联系腾讯云客服。

> 证书是否已申请过，可用以下命令确认（不影响后续步骤，`init-ssl.sh` 幂等）：

```bash
ssh ubuntu@106.53.178.130 "sudo ls /etc/letsencrypt/live/2zg.site/ 2>/dev/null && echo '证书已存在' || echo '证书不存在'"
```

### 2. 申请证书并切换 HTTPS

执行已备好的 `init-ssl.sh`。脚本会先做前置自检（docker 权限、`.env.prod` 完整性、三容器状态、80 端口可达），再自动完成证书申请、nginx 配置切换、frontend 重建三件事：

```bash
ssh ubuntu@106.53.178.130 "cd /home/ubuntu/PetShopOrder && bash deploy/init-ssl.sh 您的邮箱@example.com"
```

把 `您的邮箱@example.com` 换成真实邮箱，Let's Encrypt 会用它发送证书过期提醒。脚本行为说明：

- **幂等**：若证书已存在，跳过申请步骤，只切换 nginx 配置并重建 frontend，可安全重复执行。
- **证书落地校验**：certbot 报告成功后，脚本会检查证书文件是否真的写入 `/etc/letsencrypt/live/2zg.site/fullchain.pem`，避免无证书却切到 HTTPS 配置导致 nginx 起不来。
- **失败早**：任一自检失败立即退出并给出原因，排查指引见本文档「故障排除」。

### 3. 验证 HTTPS

逐项核对：

```bash
curl -i https://2zg.site/health       # 应返回 {"status":"UP"}
curl -i http://2zg.site/              # 应返回 301 跳转到 https
curl -i https://www.2zg.site/         # 应返回 301 跳转到根域
```

浏览器访问：

- `https://2zg.site/` → H5 首页
- `https://2zg.site/petshop-admin-7x9k2/` → Admin 登录页
- `https://2zg.site/health` → `{"status":"UP"}`

### 4. 配置证书自动续期

Let's Encrypt 证书有效期 90 天。SSH 登录后用 `crontab -e` 添加续期任务（每月 1 号凌晨 3 点）：

```
0 3 1 * * /home/ubuntu/PetShopOrder/deploy/renew-ssl.sh >> /var/log/cert-renew.log 2>&1
```

> **注意**：上面的 cron 路径是 `/home/ubuntu/PetShopOrder/deploy/renew-ssl.sh`，频率每月一次（renew-ssl.sh 注释里的 cron 示例已同步为此路径与频率）。

手动测试续期流程（不实际续期）：

```bash
ssh ubuntu@106.53.178.130 "docker run --rm -v /etc/letsencrypt:/etc/letsencrypt -v /var/www/certbot:/var/www/certbot certbot/certbot renew --dry-run"
```

> **提示**：`renew-ssl.sh` 会 `cd` 到自身所在目录（`deploy/`）再执行，`.env.prod` 与 `docker-compose.prod.yml` 同在此目录，路径自洽可用。建议启用 cron 自动续期前，先手动跑一次 `bash deploy/renew-ssl.sh` 确认整条链路（renew + nginx reload）畅通，再依赖 cron。

### 5. 配置数据备份

先创建备份目录（仅首次执行）：

```bash
sudo mkdir -p /backup && sudo chown ubuntu:ubuntu /backup
```

SSH 登录后用 `crontab -e` 添加备份任务（每天凌晨 3 点，保留 30 天）：

```
0 3 * * * docker exec petorder-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction petshop_order' > /backup/petshop-$(date +\%F).sql
10 3 * * * tar -czf /backup/uploads-$(date +\%F).tar.gz /data/petshop/uploads && find /backup -name "uploads-*" -mtime +30 -delete
20 3 * * * find /backup -name "petshop-*.sql" -mtime +30 -delete
```

> **说明**：第三行清理 30 天前的 SQL 备份（与 uploads 同策略），避免无限累积撑满磁盘。
>
> **警告**：切勿用 `tar` 直接打包运行中的 MySQL 数据目录，会得到不一致的备份。必须用 `mysqldump --single-transaction`。

---

## 上线后必做

### 腾讯地图后台配域名白名单

登录腾讯位置服务控制台 → 应用管理 → key `4UGBZ-ZMS6U-...` → 编辑 → **授权域名（referer）** 填：

```
2zg.site
localhost
```

- `2zg.site`：生产环境（H5 与 Admin 同源，一行覆盖）。
- `localhost`：本地开发（腾讯校验 referer 忽略端口，覆盖 `localhost:3000` 等任意端口）。

本项目前端用腾讯地图 JavaScript API GL（`map.qq.com/api/gljs`），走域名白名单；后端配送距离用 haversine 公式本地计算，不调腾讯 WebService API，因此**不需要配 IP 白名单**。

### 登录 Admin 改密码

访问 `https://2zg.site/petshop-admin-7x9k2/`，用初始账号登录后立即修改密码：

- 用户名：`admin`
- 初始密码：`FBzhXAwaBFAxf6iU`

### 切换真实短信验证码

确认全链路无误后，把短信从测试模式切到阿里云真实短信。

1. SSH 登录，编辑 `.env.prod`，把 `SMS_PROVIDER=log` 改为 `SMS_PROVIDER=aliyun`（AK/SK 已预填）。
2. 重启 backend：

   ```bash
   ssh ubuntu@106.53.178.130 "cd /home/ubuntu/PetShopOrder && docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d backend"
   ```

3. 用真实手机号测试验证码登录。

> **注意**：重启 backend 会清空 Sa-Token 内存会话，所有在线用户需重新登录。

### 填写飞书 webhook 与店铺坐标

登录 Admin → 系统配置页，填写：

- 飞书 webhook URL（值在本地 `~/.petshop-secrets/飞书订单通知群webhook.txt`）。
- 真实店铺经纬度（用于配送距离计算）。

---

## 凭据记录

> **警告**：以下为部署时生成的凭据，建议在 HTTPS 上线完成并记录到密码管理器后，从本文档移除明文，避免凭据长期留在代码仓库中。

| 项 | 值 |
|---|---|
| MySQL root 密码 | `QSseWGLx58JemhUiOSspVBKO` |
| Admin 初始账号 | `admin` / `FBzhXAwaBFAxf6iU` |
| Webhook AES key | `PetShop2026Order!`（兼容历史加密数据，可保留）|

MySQL 密码也保存在服务器 `/home/ubuntu/PetShopOrder/deploy/.env.prod`，可用以下命令查看：

```bash
ssh ubuntu@106.53.178.130 "docker exec petorder-mysql env | grep MYSQL_ROOT_PASSWORD"
```

---

## 备案期间可做的事

以下事项不依赖 HTTPS，备案未通过时即可完成：

- **腾讯地图后台配白名单**：见上文，随时可配。
- **数据备份 cron**：见「上线步骤 → 5」，现在就能配（数据安全不能等）。
- **通过 SSH 隧道访问后端**：在本地另开终端建立隧道，验证商品、会员、订单等接口：

  ```bash
  ssh -L 8080:localhost:8080 ubuntu@106.53.178.130
  ```

  然后本地浏览器访问 `http://localhost:8080/api/app/products` 验证接口。

- **核对商品与会员数据**：生产用的是干净版 `init.sql`（无测试商品与会员），需在 Admin 后台手工录入真实商品与会员（等 HTTPS 通后）。

---

## 故障排除

### 443 也被阻断

备案通过、证书申请成功后，如果 `https://2zg.site` 仍被劫持到 webblock（极罕见，腾讯云通常只阻断 80）：

1. 确认备案信息已关联到 `2zg.site` 与服务器 `106.53.178.130`（腾讯云备案管理页）。
2. 联系腾讯云客服，说明备案已通过但 443 仍被阻断，要求解除。
3. 临时绕过：用 DNS-01 验证申请证书（不依赖 80），但若 443 也阻断则证书用不上，必须先解除阻断。

### 证书申请失败（其他原因）

查看已申请的证书与日志：

```bash
ssh ubuntu@106.53.178.130 "docker run --rm -v /etc/letsencrypt:/etc/letsencrypt -v /var/www/certbot:/var/www/certbot certbot/certbot certificates"
```

常见原因：80 未解封、域名 DNS 未生效、防火墙。重新核对「前置条件」。

### 证书申请成功但 HTTPS 仍不通

证书已落地，但访问 `https://2zg.site` 无响应或报 SSL 错误。按顺序检查：

1. 确认 `.env.prod` 的 `NGINX_CONF` 已切到 `./nginx.conf`（`init-ssl.sh` 会自动改，但若手动改过需核对）：

   ```bash
   ssh ubuntu@106.53.178.130 "grep NGINX_CONF /home/ubuntu/PetShopOrder/deploy/.env.prod"
   ```

2. 确认 `frontend` 容器已重建并加载 HTTPS 配置：

   ```bash
   ssh ubuntu@106.53.178.130 "docker exec petorder-frontend cat /etc/nginx/conf.d/default.conf | head -5"
   ```

   首行应是 `# 完整生产配置：443 ssl ...`。若仍是「备案期间 HTTP 全功能配置」，说明容器没重建，重跑 `init-ssl.sh`。

### 容器异常

```bash
ssh ubuntu@106.53.178.130 "cd /home/ubuntu/PetShopOrder && docker compose -f deploy/docker-compose.prod.yml ps"
ssh ubuntu@106.53.178.130 "cd /home/ubuntu/PetShopOrder && docker compose -f deploy/docker-compose.prod.yml logs --tail=50 backend"
```

---

## 后续步骤

- 上线步骤全部完成后，回到「前置条件」与各步骤的 checkbox 勾选确认。
- HTTPS 上线完成后，参考 [`README.md`](./README.md) 了解日常运维（日志查看、服务更新、备份恢复）。
- 代码更新流程：本地改代码 → push Gitee 或 GitHub → 服务器 `git pull` → `docker compose ... up -d --build backend`（或 frontend）。
- **待办**：启用 cron 自动续期前，手动跑一次 `bash deploy/renew-ssl.sh` 确认 renew + nginx reload 畅通，再依赖 cron。

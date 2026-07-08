# HTTPS 上线手册（ICP 备案通过后执行）

> ✅ **状态：已完成（2026-07-08）**。`2zg.site` HTTPS 已上线，证书自动续期与数据备份 cron 已配置。本文档保留作为上线操作记录与故障排查参考。

本文档记录 `2zg.site` 在 ICP 备案通过后，完成 HTTPS 上线的完整步骤。备案期间腾讯云对未备案域名的 80 端口做了 webblock 阻断，导致 Let's Encrypt HTTP-01 验证失败；备案通过后 80 端口解封，执行本手册即可拿到证书并启用 HTTPS。

> **何时使用**：收到腾讯云备案通过通知后，按"备案后步骤"逐条执行。"备案期间可做的事"现在就能做，不必等备案。

---

## 当前状态（备案期间）

服务器三容器已在运行，后端正常，但公网暂时访问不了：

| 容器 | 状态 | 说明 |
|---|---|---|
| petorder-mysql | 运行中 | 数据已初始化（`init.sql` 自动建 16 张表 + 系统默认配置）|
| petorder-backend | 运行中（healthy）| `/health` 正常，已连上 MySQL |
| petorder-frontend | 运行中（http-only）| 仅监听 80，等待证书 |

- **80 端口**：被腾讯云 webblock 阻断，访问 `http://2zg.site` 会跳到备案提示页。
- **443 端口**：未启用（无证书）。HTTPS 通常不受未备案阻断影响，备案后实测确认。
- **容器内部**：正常，可通过 SSH 隧道访问。

---

## 前提

- ICP 备案已通过，腾讯云已解除 80 端口的 webblock 阻断。
- 服务器三容器仍在运行。如已关机，先恢复：

```bash
ssh ubuntu@106.53.178.130 "cd /home/ubuntu/PetShopOrder && docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod up -d"
```

---

## 备案后步骤

### 1. 验证 80 端口已解封

备案通过后，腾讯云会解除 80 端口阻断。确认 80 返回的是我们的 nginx，而不是 webblock 提示页：

```bash
curl -i http://2zg.site/
```

- ✅ 返回 `petshop cert init: waiting for ssl`：80 已解封，继续下一步。
- ❌ 仍跳转 `dnspod.qcloud.com/static/webblock.html`：阻断未解除，等待腾讯云生效（通常备案通过后数小时内），或联系腾讯云客服。

### 2. 申请证书并切换 HTTPS

执行已备好的 `init-ssl.sh`，它会自动完成 certbot 申请证书、切换 nginx 配置、重建 frontend 三件事：

```bash
ssh ubuntu@106.53.178.130 "cd /home/ubuntu/PetShopOrder && bash deploy/init-ssl.sh 您的邮箱@example.com"
```

把 `您的邮箱@example.com` 换成真实邮箱，Let's Encrypt 会用它发送证书过期提醒。脚本成功后，frontend 容器加载 `nginx.conf`，监听 443 并把 80 自动跳转到 443。

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

Let's Encrypt 证书有效期 90 天。SSH 登录后用 `crontab -e` 添加续期任务（每两个月 1 号凌晨 3 点）：

```
0 3 1 */2 * /home/ubuntu/PetShopOrder/deploy/renew-ssl.sh >> /var/log/cert-renew.log 2>&1
```

手动测试续期流程（不实际续期）：

```bash
ssh ubuntu@106.53.178.130 "docker run --rm -v /etc/letsencrypt:/etc/letsencrypt -v /var/www/certbot:/var/www/certbot certbot/certbot renew --dry-run"
```

### 5. 配置数据备份

SSH 登录后用 `crontab -e` 添加备份任务（每天凌晨 3 点，保留 30 天）：

```
0 3 * * * docker exec petorder-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction petshop_order' > /backup/petshop-$(date +\%F).sql
10 3 * * * tar -czf /backup/uploads-$(date +\%F).tar.gz /data/petshop/uploads && find /backup -name "uploads-*" -mtime +30 -delete
```

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

> **警告**：以下为部署时生成的凭据，请记录到密码管理器后妥善保管，建议记录完成后从本文档移除明文。

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

以下事项不依赖 HTTPS，现在就能完成：

- **腾讯地图后台配白名单**：见上文，随时可配。
- **数据备份 cron**：见"备案后步骤 → 5"，现在就能配（数据安全不能等）。
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

常见原因：80 未解封、域名 DNS 未生效、防火墙。重新核对"前提"。

### 容器异常

```bash
ssh ubuntu@106.53.178.130 "cd /home/ubuntu/PetShopOrder && docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.prod ps"
ssh ubuntu@106.53.178.130 "cd /home/ubuntu/PetShopOrder && docker compose -f deploy/docker-compose.prod.yml logs --tail=50 backend"
```

---

## 后续步骤

- 备案通过后，从"备案后步骤 → 1. 验证 80 端口已解封"开始执行。
- HTTPS 上线完成后，参考 `deploy/README.md` 了解日常运维（日志查看、服务更新、备份恢复）。
- 代码更新流程：本地改代码 → push Gitee 或 GitHub → 服务器 `git pull` → `docker compose ... up -d --build backend`（或 frontend）。

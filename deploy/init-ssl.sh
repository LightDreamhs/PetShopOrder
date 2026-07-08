#!/usr/bin/env bash
# 一键申请 Let's Encrypt 证书并切换到 HTTPS 配置。
#
# 前置（脚本会自检）：
#   1. 以 docker 组用户在服务器上执行（能跑 docker ps）
#   2. deploy/.env.prod 已填好（DB_ROOT_PASSWORD / ADMIN_INIT_PASSWORD 等）
#   3. 三容器已在运行，frontend 用 nginx.http-only.conf（NGINX_CONF 留空）
#   4. 80 端口公网可达（备案已通过、未被 webblock 阻断）
#
# 用法：bash deploy/init-ssl.sh [你的邮箱]
# 幂等：证书已存在时跳过申请，仅切换 nginx 配置并重建 frontend。
set -euo pipefail

cd "$(dirname "$0")"

DOMAIN="2zg.site"
EMAIL="${1:-admin@${DOMAIN}}"
ENV_FILE="./.env.prod"
COMPOSE_FILE="./docker-compose.prod.yml"
CERT_DIR="/etc/letsencrypt/live/${DOMAIN}"
CERT_FILE="${CERT_DIR}/fullchain.pem"

# ---------- 辅助函数 ----------

# 失败时打印提示并退出。$1: 提示信息
die() {
  echo ""
  echo "❌ 失败：$1" >&2
  echo "   排查见 deploy/HTTPS-SETUP.md「故障排除」章节。" >&2
  exit 1
}

# 步骤标题。$1: 文本
step() {
  echo ""
  echo "==> $1"
}

# 证书是否已存在。/etc/letsencrypt/live 权限 700（root），ubuntu 无权 stat，
# 借 frontend 容器（挂载 /etc/letsencrypt，容器内 root）检查。
cert_exists() {
  docker exec petorder-frontend test -f "${CERT_FILE}" 2>/dev/null
}

# ---------- 前置自检 ----------

step "0/5 前置自检"

# 0a. docker 可用（顺带确认当前用户在 docker 组）
docker ps >/dev/null 2>&1 || die "无法执行 docker ps，请以 docker 组用户在服务器执行（不要用 sudo 跑本脚本）"

# 0b. .env.prod 存在且关键变量非空
[ -f "${ENV_FILE}" ] || die "缺少 ${ENV_FILE}，请参考 .env.prod.example 创建并填好敏感值"
grep -qE '^DB_ROOT_PASSWORD=.+' "${ENV_FILE}" || die "${ENV_FILE} 中 DB_ROOT_PASSWORD 未填写"
grep -qE '^ADMIN_INIT_PASSWORD=.+' "${ENV_FILE}" || die "${ENV_FILE} 中 ADMIN_INIT_PASSWORD 未填写"

# 0c. 三容器都在运行
for c in petorder-mysql petorder-backend petorder-frontend; do
  docker inspect -f '{{.State.Running}}' "${c}" 2>/dev/null | grep -q true \
    || die "容器 ${c} 未运行，请先在 deploy 目录执行：docker compose -f ${COMPOSE_FILE} --env-file ${ENV_FILE} up -d"
done

# 0d. 80 端口可达，且 ACME 验证路由已生效（nginx 含 /.well-known/acme-challenge/ location）。
# 探测一个不存在的 challenge 文件，期望 404（路由在、文件无）；若 200 说明路由未生效（被 H5 try_files 兜底或配置缺失）。
code_main=$(curl -sS -o /dev/null -w "%{http_code}" --max-time 10 "http://${DOMAIN}/.well-known/acme-challenge/probe-init-ssl" 2>/dev/null) || true
[ "${code_main}" = "404" ] \
  || die "主域 ACME 路由未就绪（探测返回 ${code_main}，期望 404）。可能：80 被阻断 / nginx 未加载含 acme 路由的配置 / DNS 未解析"

# 0e. www 子域 ACME 路由 + DNS（证书覆盖 www.${DOMAIN}）
code_www=$(curl -sS -o /dev/null -w "%{http_code}" --max-time 10 "http://www.${DOMAIN}/.well-known/acme-challenge/probe-init-ssl" 2>/dev/null) || true
[ "${code_www}" = "404" ] \
  || die "www.${DOMAIN} ACME 路由未就绪（探测返回 ${code_www}，期望 404）。请确认 www DNS 已指向本机且 nginx 配置含 acme 路由"

echo "   自检通过。"

# ---------- 1. 申请证书 ----------

step "1/5 申请证书 ${DOMAIN} + www.${DOMAIN}（webroot 方式，邮箱 ${EMAIL}）"

if cert_exists; then
  echo "   检测到证书已存在（${CERT_FILE}），跳过申请。"
else
  docker run --rm \
    -v /etc/letsencrypt:/etc/letsencrypt \
    -v /var/www/certbot:/var/www/certbot \
    certbot/certbot certonly \
    --webroot -w /var/www/certbot \
    -d "${DOMAIN}" -d "www.${DOMAIN}" \
    --email "${EMAIL}" --agree-tos --no-eff-email --non-interactive \
    || die "certbot 申请证书失败。检查 80 端口是否解封、DNS 是否解析到本机"
fi

# 1b. 证书落地校验（避免无证书却切到 HTTPS 配置，导致 nginx 起不来）
cert_exists || die "certbot 报告成功，但 ${CERT_FILE} 不可访问。检查 frontend 容器是否挂载了 /etc/letsencrypt"
echo "   证书就位：${CERT_FILE}"

# ---------- 2. 切换 nginx 配置 ----------

step "2/5 切换 nginx 到 HTTPS 配置（NGINX_CONF → ./nginx.conf）"

if grep -q '^NGINX_CONF=' "${ENV_FILE}"; then
  sed -i 's|^NGINX_CONF=.*|NGINX_CONF=./nginx.conf|' "${ENV_FILE}"
else
  echo 'NGINX_CONF=./nginx.conf' >> "${ENV_FILE}"
fi
echo "   已更新 ${ENV_FILE}：NGINX_CONF=./nginx.conf"

# ---------- 3. 重建 frontend ----------

step "3/5 重建 frontend 容器以加载 HTTPS 配置"

docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --force-recreate frontend \
  || die "重建 frontend 失败。可用 docker compose ... logs frontend 查看原因"

# ---------- 4. 等待并验证 ----------

step "4/5 等待 frontend 就绪并验证 HTTPS"

# 用 --resolve 连本地 443（SNI/Host 仍为域名），绕过服务器 hairpin NAT，验证 nginx+证书+backend 本地链路。
ok=0
for i in 1 2 3 4 5 6; do
  if curl -fsS --resolve "${DOMAIN}:443:127.0.0.1" --max-time 10 "https://${DOMAIN}/health" >/dev/null 2>&1; then
    ok=1
    break
  fi
  echo "   frontend 尚未就绪，5 秒后重试（${i}/6）..."
  sleep 5
done

if [ "$ok" = 1 ]; then
  echo "   ✅ HTTPS 本地链路就绪（nginx + 证书 + backend）。外部访问请从本地电脑验证 https://${DOMAIN}/"
else
  echo "   ⚠️  HTTPS 本地验证未通过（已重试 6 次，约 30 秒）。证书已申请、nginx 已切 HTTPS 配置。请排查："
  echo "     ① frontend 是否加载 nginx.conf：docker compose -f ${COMPOSE_FILE} logs --tail=50 frontend"
  echo "     ② 证书是否落地：sudo ls /etc/letsencrypt/live/${DOMAIN}/"
  echo "     ③ 腾讯云安全组是否放行入站 443（影响外部访问）"
  echo "   外部验证（从本地电脑）：curl -i https://${DOMAIN}/health"
  echo "   注：服务器自身 curl 公网 https 可能因 hairpin NAT 返回 000，属正常；用 --resolve 或从外部验证。"
fi

# ---------- 5. 完成 ----------

step "5/5 完成"
echo "请访问验证："
echo "   https://${DOMAIN}/                     （H5 首页）"
echo "   https://${DOMAIN}/petshop-admin-7x9k2/ （Admin 登录）"
echo "   http://${DOMAIN}/                      （应自动跳 HTTPS）"
echo "   https://www.${DOMAIN}/                 （应跳根域）"
echo ""
echo "下一步：配置证书自动续期 cron（见 deploy/HTTPS-SETUP.md 第 4 步）。"

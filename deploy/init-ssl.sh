#!/usr/bin/env bash
# 一键申请 Let's Encrypt 证书并切换到 HTTPS 配置。
#
# 前置：
#   1. 域名 DNS 已解析到本机（2zg.site / www.2zg.site → 本服务器 IP），80 端口可外网访问
#   2. deploy/.env.prod 已填好（DB_ROOT_PASSWORD / ADMIN_INIT_PASSWORD 等）
#   3. frontend 容器已用 nginx.http-only.conf 启动（NGINX_CONF 留空）
#
# 用法：bash deploy/init-ssl.sh [你的邮箱]
# （docker 命令需要当前用户在 docker 组；若权限不足，请在前面加 sudo）
set -euo pipefail

cd "$(dirname "$0")"

DOMAIN="2zg.site"
EMAIL="${1:-admin@${DOMAIN}}"

echo "==> 1/3 申请证书 ${DOMAIN} + www.${DOMAIN}（webroot 方式）"
docker run --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/www/certbot:/var/www/certbot \
  certbot/certbot certonly \
  --webroot -w /var/www/certbot \
  -d "${DOMAIN}" -d "www.${DOMAIN}" \
  --email "${EMAIL}" --agree-tos --no-eff-email --non-interactive

echo "==> 2/3 切换 nginx 到 HTTPS 配置（.env.prod 中 NGINX_CONF → ./nginx.conf）"
if grep -q '^NGINX_CONF=' .env.prod; then
  sed -i 's|^NGINX_CONF=.*|NGINX_CONF=./nginx.conf|' .env.prod
else
  echo 'NGINX_CONF=./nginx.conf' >> .env.prod
fi

echo "==> 3/3 重建 frontend 容器以加载 HTTPS 配置"
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --force-recreate frontend

echo ""
echo "✅ 完成。请访问验证："
echo "   https://${DOMAIN}/                     （H5 首页）"
echo "   https://${DOMAIN}/petshop-admin-7x9k2/ （Admin 登录）"
echo "   http://${DOMAIN}/                      （应自动跳 HTTPS）"
echo "   https://www.${DOMAIN}/                 （应跳根域）"

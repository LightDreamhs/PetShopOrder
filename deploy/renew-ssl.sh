#!/usr/bin/env bash
# Let's Encrypt 证书续期：certbot renew + reload nginx。
# 建议 cron 每月执行一次：0 3 1 * * /home/ubuntu/PetShopOrder/deploy/renew-ssl.sh >> /var/log/cert-renew.log 2>&1
set -euo pipefail

cd "$(dirname "$0")"

echo "==> certbot renew"
docker run --rm \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/www/certbot:/var/www/certbot \
  certbot/certbot renew --quiet

echo "==> reload nginx"
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T frontend nginx -s reload

echo "✅ 续期完成 $(date)"

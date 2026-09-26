#!/usr/bin/env bash
# 应急：在服务器上用 node 容器构建 admin 前端（本地机器不可用时用）。
# 前提：服务器已 git pull 到最新代码（vite.config.ts 的 base 为写死版本）。
# 说明：
# - 2C2G + swap(4G) 实测可跑通，但比本地慢，仅作应急，日常仍走本地构建 + 上传产物；
# - pnpm build 末尾自动执行 scripts/check-dist.mjs 产物自检，污染产物无法出线；
# - H5 不在本脚本范围：线上 H5 产物可能含站外修复，重建前必须人工确认（见 deploy/README.md）。
set -euo pipefail

cd "$(dirname "$0")/../frontend/admin"

docker run --rm \
  -v "$PWD":/app -w /app \
  -e NODE_OPTIONS=--max-old-space-size=1536 \
  node:22-alpine \
  sh -c 'npm i -g pnpm@11.9.0 --registry=https://registry.npmmirror.com && pnpm install --frozen-lockfile && pnpm build'

echo ""
echo "✅ admin 构建完成且自检通过。重建 frontend 容器："
echo "  cd ~/PetShopOrder/deploy && docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build frontend"

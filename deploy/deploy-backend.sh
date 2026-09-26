#!/bin/bash
# 部署 backend：本地构建 jar → 上传 → 服务器组装运行镜像 → 重建容器
# 背景：服务器 2C2G 跑不动镜像内 Maven 构建（Maven Central 直连 ~126KB/s，
#       全量拉依赖 >1 小时且易 OOM），与前端同策略：本地构建、服务器只组装。
# 用法：bash deploy/deploy-backend.sh
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SERVER="ubuntu@106.53.178.130"
REMOTE_ROOT="~/PetShopOrder"

echo "==== [1/5] 本地打包 backend（mvn clean package -DskipTests）===="
cd "$ROOT_DIR/backend"
mvn -q clean package -DskipTests
JAR="$ROOT_DIR/backend/target/app.jar"
if [ ! -f "$JAR" ]; then
    echo "[ERROR] 未找到 $JAR"
    exit 1
fi

echo "==== [2/5] 上传 jar 与运行时 Dockerfile ===="
scp "$JAR" "$SERVER:$REMOTE_ROOT/backend/app-deploy.jar"
scp "$ROOT_DIR/backend/Dockerfile.runtime" "$SERVER:$REMOTE_ROOT/backend/Dockerfile.runtime"

echo "==== [3/5] 服务器组装运行时镜像（跳过 Maven，秒级）===="
ssh "$SERVER" "cd $REMOTE_ROOT/backend && docker build -f Dockerfile.runtime -t deploy-backend:latest ."

echo "==== [4/5] 重建 backend 容器（--no-deps 防止 compose 连带构建其它服务）===="
ssh "$SERVER" "cd $REMOTE_ROOT/deploy && docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --no-deps --no-build backend"

echo "==== [5/5] 健康检查（等待启动 + healthcheck）===="
sleep 45
for i in 1 2 3 4 5; do
    health="$(ssh "$SERVER" "docker inspect --format '{{.State.Health.Status}}' petorder-backend")"
    if [ "$health" = "healthy" ]; then
        echo "[SUCCESS] backend 已上线（healthy）"
        exit 0
    fi
    echo "当前 health=$health，等待重试 ($i/5)..."
    sleep 15
done

echo "[ERROR] backend 未通过健康检查，请查看日志："
echo "  ssh $SERVER \"cd $REMOTE_ROOT/deploy && docker compose -f docker-compose.prod.yml logs -f backend\""
exit 1

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/docker-compose.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "未找到 docker，请先安装 Docker Desktop 或 Docker Engine。"
  exit 1
fi

echo "使用 Docker Compose 启动微服务后端栈..."
docker compose -f "$COMPOSE_FILE" up -d

echo "后端服务已提交启动："
echo "网关：http://localhost:8080"
echo "认证：http://localhost:8101"
echo "用户：http://localhost:8102"
echo "就诊：http://localhost:8103"
echo "病历：http://localhost:8104"
echo "诊疗：http://localhost:8105"
echo "工作流：http://localhost:8106"
echo "费用：http://localhost:8107"
echo "系统：http://localhost:8108"
echo "AI：http://localhost:8109"

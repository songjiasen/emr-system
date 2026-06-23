#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/docker-compose.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "未找到 docker，请先安装 Docker Desktop 或 Docker Engine。"
  exit 1
fi

echo "停止 Docker Compose 微服务后端栈..."
docker compose -f "$COMPOSE_FILE" down --remove-orphans

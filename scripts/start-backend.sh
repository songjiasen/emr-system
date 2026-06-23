#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/tmp/backend-logs"
mkdir -p "$LOG_DIR"

if [ -d "/opt/homebrew/opt/openjdk/bin" ]; then
  export PATH="/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:$PATH"
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "未找到 mvn，请先安装 Maven 后再启动后端，或改用 scripts/start-backend-docker.sh。"
  exit 1
fi

services=(
  emr-auth-service
  emr-user-service
  emr-visit-service
  emr-record-service
  emr-clinical-service
  emr-workflow-service
  emr-billing-service
  emr-system-service
  emr-ai-service
  emr-gateway
)

echo "安装公共模块 emr-common ..."
(cd "$ROOT_DIR/backend" && mvn -q -pl emr-common -DskipTests install)

for service in "${services[@]}"; do
  echo "启动 $service ..."
  (
    cd "$ROOT_DIR/backend"
    nohup mvn -pl "$service" spring-boot:run > "$LOG_DIR/$service.log" 2>&1 &
    echo "$!" > "$LOG_DIR/$service.pid"
  )
done

echo "后端启动命令已发出，日志目录：$LOG_DIR"
echo "网关默认地址：http://localhost:8080"

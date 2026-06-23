#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ -d "/opt/homebrew/opt/openjdk/bin" ]; then
  export PATH="/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:$PATH"
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "未找到 mvn，请先安装 Maven 后再构建后端；当前环境也可用 Docker 方式单独执行后端测试。"
  exit 1
fi

echo "构建后端微服务..."
(cd "$ROOT_DIR/backend" && mvn test)

for app in emr-frontend manage_code; do
  APP_DIR="$ROOT_DIR/frontend/$app"
  if [ ! -d "$APP_DIR/node_modules" ]; then
    echo "未找到 $app/node_modules，请先在 $APP_DIR 执行 npm install。"
    exit 1
  fi

  echo "构建前端 $app..."
  (cd "$APP_DIR" && npm run build)
done

echo "全部构建完成。"

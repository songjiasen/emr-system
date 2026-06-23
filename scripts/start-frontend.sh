#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/tmp/frontend-logs"
mkdir -p "$LOG_DIR"

apps=(
  "emr-frontend:5173"
  "manage_code:5174"
)

for item in "${apps[@]}"; do
  app="${item%%:*}"
  port="${item##*:}"
  app_dir="$ROOT_DIR/frontend/$app"

  if [ ! -d "$app_dir/node_modules" ]; then
    echo "未找到 $app/node_modules，请先在 $app_dir 执行 npm install。"
    exit 1
  fi

  echo "启动 $app，端口 $port ..."
  (
    cd "$app_dir"
    nohup npm run dev -- --host 0.0.0.0 --port "$port" > "$LOG_DIR/$app.log" 2>&1 &
    echo "$!" > "$LOG_DIR/$app.pid"
  )
done

echo "患者端：http://localhost:5173"
echo "后台端：http://localhost:5174"

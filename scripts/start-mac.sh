#!/usr/bin/env bash
set -euo pipefail

MYSQL_HOST="127.0.0.1"
MYSQL_PORT="3306"
MYSQL_USER="root"
MYSQL_PASSWORD="root"
SKIP_DB_INIT=0
SKIP_NPM_INSTALL=0

usage() {
  cat <<'EOF'
Usage:
  scripts/start-mac.sh [options]

Options:
  --mysql-host HOST        MySQL host, default: 127.0.0.1
  --mysql-port PORT        MySQL port, default: 3306
  --mysql-user USER        MySQL user, default: root
  --mysql-password PASS    MySQL password, default: root
  --skip-db-init           Skip deploy/mysql/init/*.sql
  --skip-npm-install       Do not run npm install when node_modules is missing
  -h, --help               Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mysql-host)
      MYSQL_HOST="${2:-}"
      shift 2
      ;;
    --mysql-port)
      MYSQL_PORT="${2:-}"
      shift 2
      ;;
    --mysql-user)
      MYSQL_USER="${2:-}"
      shift 2
      ;;
    --mysql-password)
      MYSQL_PASSWORD="${2:-}"
      shift 2
      ;;
    --skip-db-init)
      SKIP_DB_INIT=1
      shift
      ;;
    --skip-npm-install)
      SKIP_NPM_INSTALL=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
DB_INIT_DIR="$ROOT_DIR/deploy/mysql/init"
LOG_DIR="$ROOT_DIR/tmp/mac-logs"
mkdir -p "$LOG_DIR"

if [[ -d "/opt/homebrew/opt/openjdk/bin" ]]; then
  export PATH="/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:$PATH"
fi

if [[ -d "/usr/local/opt/openjdk/bin" ]]; then
  export PATH="/usr/local/opt/openjdk/bin:/usr/local/bin:$PATH"
fi

step() {
  printf '\n==> %s\n' "$1"
}

ok() {
  printf 'OK  %s\n' "$1"
}

fail() {
  printf 'ERROR  %s\n' "$1" >&2
  exit 1
}

require_command() {
  local name="$1"
  local hint="$2"
  if ! command -v "$name" >/dev/null 2>&1; then
    fail "$name was not found. $hint"
  fi
}

major_version() {
  local text="$1"
  local number
  number="$(printf '%s\n' "$text" | sed -E 's/^[^0-9]*([0-9]+).*/\1/')"
  if [[ "$number" =~ ^[0-9]+$ ]]; then
    printf '%s\n' "$number"
  else
    printf '0\n'
  fi
}

port_in_use() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

mysql_args() {
  local args=(-h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER")
  if [[ -n "$MYSQL_PASSWORD" ]]; then
    args+=("-p$MYSQL_PASSWORD")
  fi
  printf '%s\0' "${args[@]}"
}

run_mysql() {
  local args=()
  while IFS= read -r -d '' item; do
    args+=("$item")
  done < <(mysql_args)
  mysql "${args[@]}" "$@"
}

start_logged_process() {
  local name="$1"
  local workdir="$2"
  shift 2

  local stdout="$LOG_DIR/$name.out.log"
  local stderr="$LOG_DIR/$name.err.log"
  local pidfile="$LOG_DIR/$name.pid"
  rm -f "$stdout" "$stderr" "$pidfile"

  (
    cd "$workdir"
    nohup "$@" >"$stdout" 2>"$stderr" &
    echo "$!" >"$pidfile"
  )
  ok "$name started, pid=$(cat "$pidfile")"
}

wait_http_ok() {
  local name="$1"
  local url="$2"
  local timeout_seconds="${3:-180}"
  local start_ts
  start_ts="$(date +%s)"

  while (( "$(date +%s)" - start_ts < timeout_seconds )); do
    if curl -sf --max-time 3 "$url" >/dev/null 2>&1; then
      ok "$name is ready"
      return 0
    fi
    sleep 2
  done

  printf '\nLast logs for %s:\n' "$name" >&2
  for file in "$LOG_DIR/$name".*.log; do
    [[ -f "$file" ]] || continue
    printf -- '--- %s ---\n' "$(basename "$file")" >&2
    tail -n 40 "$file" >&2 || true
  done
  fail "$name did not become ready in ${timeout_seconds}s. Check logs in $LOG_DIR"
}

step "Checking required tools"
require_command java "Install JDK 17. On macOS, Homebrew users can run: brew install openjdk"
require_command mvn "Install Maven. On macOS, Homebrew users can run: brew install maven"
require_command node "Install Node.js 18 or newer. On macOS, Homebrew users can run: brew install node"
require_command npm "Install Node.js 18 or newer."
require_command mysql "Install MySQL Client. On macOS, Homebrew users can run: brew install mysql-client or mysql"
require_command lsof "lsof is required for port checks."
require_command curl "curl is required for health checks."

JAVA_LINE="$(java -version 2>&1 | head -n 1)"
JAVA_MAJOR="$(major_version "$JAVA_LINE")"
if (( JAVA_MAJOR < 17 )); then
  fail "Java 17 or newer is required. Current: $JAVA_LINE"
fi
ok "Java: $JAVA_LINE"

NODE_LINE="$(node --version)"
NODE_MAJOR="$(major_version "$NODE_LINE")"
if (( NODE_MAJOR < 18 )); then
  fail "Node.js 18 or newer is required. Current: $NODE_LINE"
fi
ok "Node.js: $NODE_LINE"
ok "Maven: $(command -v mvn)"
ok "npm: $(command -v npm)"
ok "mysql: $(command -v mysql)"

step "Checking application ports"
APP_PORTS=(8080 8101 8102 8103 8104 8105 8106 8107 8108 8109 5173 5174)
for port in "${APP_PORTS[@]}"; do
  if port_in_use "$port"; then
    fail "Port $port is already in use. Stop the process using this port and run this script again."
  fi
done
ok "Application ports are free"

step "Checking MySQL connection"
if ! run_mysql -e "SELECT 1;" >/dev/null; then
  fail "Cannot connect to MySQL at $MYSQL_HOST:$MYSQL_PORT with user $MYSQL_USER"
fi
ok "MySQL is reachable at $MYSQL_HOST:$MYSQL_PORT"

if [[ "$SKIP_DB_INIT" -eq 0 ]]; then
  step "Initializing MySQL databases"
  for sql_file in "$DB_INIT_DIR"/*.sql; do
    [[ -f "$sql_file" ]] || continue
    printf 'Running %s\n' "$(basename "$sql_file")"
    run_mysql <"$sql_file"
  done
  ok "Database init scripts completed"
fi

step "Installing common backend module"
(cd "$BACKEND_DIR" && mvn -q -pl emr-common -DskipTests install)
ok "emr-common installed"

step "Installing frontend dependencies when needed"
FRONTEND_APPS=("emr-frontend:5173" "manage_code:5174")
for item in "${FRONTEND_APPS[@]}"; do
  app="${item%%:*}"
  app_dir="$FRONTEND_DIR/$app"
  if [[ ! -d "$app_dir/node_modules" ]]; then
    if [[ "$SKIP_NPM_INSTALL" -eq 1 ]]; then
      fail "$app/node_modules was not found and --skip-npm-install was set."
    fi
    printf 'Installing dependencies for %s\n' "$app"
    (cd "$app_dir" && npm install)
  else
    ok "$app dependencies exist"
  fi
done

DB_URL_SUFFIX="?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"

step "Starting backend services"
start_logged_process "emr-auth-service" "$BACKEND_DIR" env \
  "EMR_AUTH_DB_URL=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/emr_auth$DB_URL_SUFFIX" \
  "EMR_AUTH_DB_USERNAME=$MYSQL_USER" \
  "EMR_AUTH_DB_PASSWORD=$MYSQL_PASSWORD" \
  mvn -q -pl emr-auth-service spring-boot:run

start_logged_process "emr-user-service" "$BACKEND_DIR" env \
  "EMR_USER_DB_URL=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/emr_user$DB_URL_SUFFIX" \
  "EMR_USER_DB_USERNAME=$MYSQL_USER" \
  "EMR_USER_DB_PASSWORD=$MYSQL_PASSWORD" \
  mvn -q -pl emr-user-service spring-boot:run

start_logged_process "emr-visit-service" "$BACKEND_DIR" env \
  "EMR_VISIT_DB_URL=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/emr_visit$DB_URL_SUFFIX" \
  "EMR_VISIT_DB_USERNAME=$MYSQL_USER" \
  "EMR_VISIT_DB_PASSWORD=$MYSQL_PASSWORD" \
  mvn -q -pl emr-visit-service spring-boot:run

start_logged_process "emr-record-service" "$BACKEND_DIR" env \
  "EMR_RECORD_DB_URL=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/emr_record$DB_URL_SUFFIX" \
  "EMR_RECORD_DB_USERNAME=$MYSQL_USER" \
  "EMR_RECORD_DB_PASSWORD=$MYSQL_PASSWORD" \
  mvn -q -pl emr-record-service spring-boot:run

start_logged_process "emr-clinical-service" "$BACKEND_DIR" env \
  "EMR_CLINICAL_DB_URL=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/emr_clinical$DB_URL_SUFFIX" \
  "EMR_CLINICAL_DB_USERNAME=$MYSQL_USER" \
  "EMR_CLINICAL_DB_PASSWORD=$MYSQL_PASSWORD" \
  mvn -q -pl emr-clinical-service spring-boot:run

start_logged_process "emr-workflow-service" "$BACKEND_DIR" env \
  "EMR_WORKFLOW_DB_URL=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/emr_workflow$DB_URL_SUFFIX" \
  "EMR_WORKFLOW_DB_USERNAME=$MYSQL_USER" \
  "EMR_WORKFLOW_DB_PASSWORD=$MYSQL_PASSWORD" \
  "EMR_CLINICAL_BASE_URL=http://127.0.0.1:8105" \
  "EMR_RECORD_BASE_URL=http://127.0.0.1:8104" \
  mvn -q -pl emr-workflow-service spring-boot:run

start_logged_process "emr-billing-service" "$BACKEND_DIR" env \
  "EMR_BILLING_DB_URL=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/emr_billing$DB_URL_SUFFIX" \
  "EMR_BILLING_DB_USERNAME=$MYSQL_USER" \
  "EMR_BILLING_DB_PASSWORD=$MYSQL_PASSWORD" \
  mvn -q -pl emr-billing-service spring-boot:run

start_logged_process "emr-system-service" "$BACKEND_DIR" env \
  "EMR_SYSTEM_DB_URL=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/emr_system$DB_URL_SUFFIX" \
  "EMR_SYSTEM_DB_USERNAME=$MYSQL_USER" \
  "EMR_SYSTEM_DB_PASSWORD=$MYSQL_PASSWORD" \
  mvn -q -pl emr-system-service spring-boot:run

start_logged_process "emr-ai-service" "$BACKEND_DIR" env \
  "EMR_AI_DB_URL=jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/emr_ai$DB_URL_SUFFIX" \
  "EMR_AI_DB_USERNAME=$MYSQL_USER" \
  "EMR_AI_DB_PASSWORD=$MYSQL_PASSWORD" \
  "EMR_RECORD_BASE_URL=http://127.0.0.1:8104" \
  "EMR_AI_PROVIDER=mock" \
  mvn -q -pl emr-ai-service spring-boot:run

start_logged_process "emr-gateway" "$BACKEND_DIR" env \
  "EMR_AUTH_SERVICE_URL=http://127.0.0.1:8101" \
  "EMR_USER_SERVICE_URL=http://127.0.0.1:8102" \
  "EMR_VISIT_SERVICE_URL=http://127.0.0.1:8103" \
  "EMR_RECORD_SERVICE_URL=http://127.0.0.1:8104" \
  "EMR_CLINICAL_SERVICE_URL=http://127.0.0.1:8105" \
  "EMR_WORKFLOW_SERVICE_URL=http://127.0.0.1:8106" \
  "EMR_BILLING_SERVICE_URL=http://127.0.0.1:8107" \
  "EMR_SYSTEM_SERVICE_URL=http://127.0.0.1:8108" \
  "EMR_AI_SERVICE_URL=http://127.0.0.1:8109" \
  "EMR_AUTH_SERVICE_BASE_URL=http://127.0.0.1:8101" \
  "EMR_AUDIT_SYSTEM_SERVICE_BASE_URL=http://127.0.0.1:8108" \
  mvn -q -pl emr-gateway spring-boot:run

step "Waiting for backend health checks"
wait_http_ok "emr-auth-service" "http://127.0.0.1:8101/health" 240
wait_http_ok "emr-user-service" "http://127.0.0.1:8102/health" 240
wait_http_ok "emr-visit-service" "http://127.0.0.1:8103/health" 240
wait_http_ok "emr-record-service" "http://127.0.0.1:8104/health" 240
wait_http_ok "emr-clinical-service" "http://127.0.0.1:8105/health" 240
wait_http_ok "emr-workflow-service" "http://127.0.0.1:8106/health" 240
wait_http_ok "emr-billing-service" "http://127.0.0.1:8107/health" 240
wait_http_ok "emr-system-service" "http://127.0.0.1:8108/health" 240
wait_http_ok "emr-ai-service" "http://127.0.0.1:8109/health" 240
wait_http_ok "emr-gateway" "http://127.0.0.1:8080/health" 240

step "Starting frontend apps"
for item in "${FRONTEND_APPS[@]}"; do
  app="${item%%:*}"
  port="${item##*:}"
  start_logged_process "$app" "$FRONTEND_DIR/$app" npm run dev -- --host 0.0.0.0 --port "$port"
done

step "Waiting for frontend pages"
wait_http_ok "emr-frontend" "http://127.0.0.1:5173/" 90
wait_http_ok "manage_code" "http://127.0.0.1:5174/" 90

cat <<EOF

EMR system is running.
Patient app: http://localhost:5173
Admin app:   http://localhost:5174
Gateway:     http://localhost:8080

Demo accounts:
patient_demo / 123456      role: patient
admin        / admin123    role: admin
doctor       / 123456      role: doctor
nurse        / 123456      role: nurse
director     / 123456      role: director

Logs and pid files: $LOG_DIR
To stop services, run:
  cat "$LOG_DIR"/*.pid | xargs kill
EOF

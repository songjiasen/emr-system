#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "检查演示入口..."
node scripts/verify-demo-surface.mjs

echo "检查前端 JS 语法..."
find frontend -name '*.js' -exec node --check {} \;
node --check scripts/verify-demo-surface.mjs
node --check scripts/verify-vue-scripts.mjs
node scripts/verify-vue-scripts.mjs

echo "检查后端 POM XML..."
python3 - <<'PY'
import xml.etree.ElementTree as ET
from pathlib import Path

for path in Path("backend").glob("**/pom.xml"):
    ET.parse(path)

print("pom xml ok")
PY

echo "检查 Java 文件基础结构..."
python3 - <<'PY'
from pathlib import Path

errors = []
for path in Path("backend").glob("**/src/main/java/**/*.java"):
    text = path.read_text()
    if text.count("{") != text.count("}"):
        errors.append(f"{path}: 大括号数量不匹配")
    if not text.lstrip().startswith("package "):
        errors.append(f"{path}: 缺少 package 声明")

if errors:
    raise SystemExit("\n".join(errors))

print("java shape ok")
PY

echo "检查前端 API 导入..."
python3 - <<'PY'
from pathlib import Path
import re

errors = []
for path in Path("frontend").glob("**/src/App.vue"):
    text = path.read_text()
    for spec in re.findall(r"from './api/([^']+)'", text):
        if not (path.parent / "api" / f"{spec}.js").exists():
            errors.append(f"{path}: 缺少 api/{spec}.js")

if errors:
    raise SystemExit("\n".join(errors))

print("frontend api imports ok")
PY

echo "检查 SQL 初始化脚本..."
python3 - <<'PY'
from pathlib import Path

sql_dir = Path("deploy/mysql/init")
files = sorted(sql_dir.glob("*.sql"))
prefixes = [path.name.split("-", 1)[0] for path in files]
expected = [f"{index:02d}" for index in range(len(prefixes))]

if prefixes != expected:
    raise SystemExit(f"SQL 文件序号不连续: {prefixes}")

text = "\n".join(path.read_text() for path in files)
if "TEXT DEFAULT NULL" in text or "LONGTEXT DEFAULT NULL" in text:
    raise SystemExit("MySQL 5.7 不允许 TEXT/LONGTEXT 显式 DEFAULT NULL")

print("sql static ok")
PY

echo "检查 Map 请求体参数安全..."
if rg -q "request\.(get|getOrDefault|containsKey|forEach)\(" backend --glob '**/src/main/**/*.java'; then
  rg -n "request\.(get|getOrDefault|containsKey|forEach)\(" backend --glob '**/src/main/**/*.java'
  echo "发现直接访问 request 的代码，请改为 safePayload 后再取字段。"
  exit 1
fi

if rg -q "@RequestBody Map<String, Object>" backend --glob '**/src/main/**/*.java'; then
  rg -n "@RequestBody Map<String, Object>" backend --glob '**/src/main/**/*.java'
  echo "发现必填 Map 请求体，请改为 @RequestBody(required = false)。"
  exit 1
fi

echo "静态检查完成。"

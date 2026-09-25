#!/usr/bin/env bash
# 一键启动：PostgreSQL + Spring Boot 后端 + React 前端（全部用户态，无需 root）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="$ROOT/tools/jdk"
export PATH="$JAVA_HOME/bin:$ROOT/tools/maven/bin:$PATH"
PGBIN="$ROOT/tools/pg-debs/root/usr/lib/postgresql/15/bin"
export PGDATA="$ROOT/pgdata"

# 1. PostgreSQL
if ! "$PGBIN/pg_ctl" -D "$PGDATA" status >/dev/null 2>&1; then
  echo "==> 启动 PostgreSQL (localhost:5432)"
  "$PGBIN/pg_ctl" -D "$PGDATA" -l "$ROOT/pgdata.log" \
    -o "-p 5432 -k /tmp -c listen_addresses=localhost" start
else
  echo "==> PostgreSQL 已在运行"
fi

# 2. 后端
echo "==> 启动 Spring Boot 后端 (localhost:8080)"
(cd "$ROOT/backend" && mvn -q spring-boot:run) &
BACKEND_PID=$!

# 3. 前端
echo "==> 启动 Vite 前端 (http://localhost:5173)"
(cd "$ROOT/frontend" && npm run dev) &
FRONTEND_PID=$!

trap 'kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true' EXIT
wait

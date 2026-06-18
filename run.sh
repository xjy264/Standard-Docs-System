#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$ROOT_DIR/deploy"
ENV_FILE="$DEPLOY_DIR/.env"
ENV_EXAMPLE="$DEPLOY_DIR/.env.example"

command="${1:-start}"

print_help() {
  cat <<'EOF'
大同房建公寓段标准化资料管理系统启动脚本

用法：
  ./run.sh                 一键启动 Docker 全套服务
  ./run.sh start           启动 MySQL、Redis、MinIO、后端、前端
  ./run.sh stop            停止服务
  ./run.sh restart         重启服务
  ./run.sh status          查看服务状态
  ./run.sh logs            查看服务日志
  ./run.sh build           重新构建并启动服务
  ./run.sh migrate         对已有数据库执行幂等迁移脚本
  ./run.sh local           本地开发启动：Docker 启动 MySQL/Redis/MinIO，本机启动前后端
  ./run.sh help            查看帮助

默认账号：
  用户名：admin
  密码：Admin12345@@
EOF
}

ensure_env() {
  if [ ! -f "$ENV_FILE" ]; then
    if [ ! -f "$ENV_EXAMPLE" ]; then
      echo "缺少 ${ENV_EXAMPLE}，无法生成部署环境配置。"
      exit 1
    fi
    cp "$ENV_EXAMPLE" "$ENV_FILE"
    echo "已生成 ${ENV_FILE}，请按需修改域名、端口和密码。"
  fi
}

compose_cmd() {
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    docker compose "$@"
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return
  fi
  echo "未检测到 Docker Compose。请先安装 Docker Desktop 或 docker compose 插件。"
  exit 1
}

migration_files=(
  "13-doc-recycle-bin.sql"
  "14-doc-submission-soft-delete.sql"
)

env_value() {
  local name="$1"
  local default_value="${2:-}"
  local current_value="${!name:-}"
  if [ -n "$current_value" ]; then
    printf '%s' "$current_value"
    return
  fi
  if [ -f "$ENV_FILE" ]; then
    local file_value
    file_value="$(grep -E "^${name}=" "$ENV_FILE" | tail -n 1 | cut -d= -f2- || true)"
    if [ -n "$file_value" ]; then
      file_value="${file_value%\"}"
      file_value="${file_value#\"}"
      printf '%s' "$file_value"
      return
    fi
  fi
  printf '%s' "$default_value"
}

parse_mysql_url() {
  local url="${MYSQL_URL:-}"
  parsed_mysql_host=""
  parsed_mysql_port=""
  parsed_mysql_database=""
  if [[ "$url" =~ jdbc:mysql://([^:/?]+)(:([0-9]+))?/([^?]+) ]]; then
    parsed_mysql_host="${BASH_REMATCH[1]}"
    parsed_mysql_port="${BASH_REMATCH[3]:-3306}"
    parsed_mysql_database="${BASH_REMATCH[4]}"
  fi
}

wait_compose_mysql() {
  echo "等待 MySQL 就绪..."
  for _ in {1..60}; do
    if compose_cmd exec -T mysql sh -lc 'MYSQL_PWD="$MYSQL_PASSWORD" mysqladmin ping -h127.0.0.1 -u"$MYSQL_USER" --silent' >/dev/null 2>&1; then
      return
    fi
    sleep 2
  done
  echo "MySQL 启动超时，请检查 docker compose logs mysql。"
  exit 1
}

migrate_compose_mysql() {
  cd "$DEPLOY_DIR"
  compose_cmd up -d mysql
  wait_compose_mysql
  for file in "${migration_files[@]}"; do
    echo "执行数据库迁移：$file"
    compose_cmd exec -T mysql sh -lc 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"' < "$DEPLOY_DIR/mysql-init/$file" >/dev/null
  done
}

migrate_external_mysql() {
  parse_mysql_url
  local mysql_host="${MYSQL_HOST:-$parsed_mysql_host}"
  local mysql_port="${MYSQL_PORT:-$parsed_mysql_port}"
  local mysql_database="${MYSQL_DATABASE:-$parsed_mysql_database}"
  local mysql_user
  local mysql_password
  mysql_port="${mysql_port:-3306}"
  mysql_database="${mysql_database:-$(env_value MYSQL_DATABASE dt_standard_system)}"
  mysql_user="$(env_value MYSQL_USER standard)"
  mysql_password="$(env_value MYSQL_PASSWORD)"
  if [ "$mysql_host" = "localhost" ] || [ "$mysql_host" = "127.0.0.1" ]; then
    mysql_host="host.docker.internal"
  fi
  if [ -z "$mysql_host" ] || [ -z "$mysql_password" ]; then
    echo "外部数据库迁移需要设置 MYSQL_HOST 和 MYSQL_PASSWORD，或提供可解析的 MYSQL_URL。"
    exit 1
  fi
  for file in "${migration_files[@]}"; do
    echo "执行数据库迁移：$file"
    docker run --rm -i \
      -e MYSQL_PWD="$mysql_password" \
      mysql:8.0 \
      mysql -h"$mysql_host" -P"$mysql_port" -u"$mysql_user" "$mysql_database" < "$DEPLOY_DIR/mysql-init/$file" >/dev/null
  done
}

migrate_db() {
  ensure_env
  if [ -n "${MYSQL_HOST:-}" ] || [ -n "${MYSQL_URL:-}" ]; then
    migrate_external_mysql
  else
    migrate_compose_mysql
  fi
  echo "数据库迁移执行完成。"
}

start_all() {
  ensure_env
  cd "$DEPLOY_DIR"
  compose_cmd up -d
  echo
  echo "服务启动命令已执行。"
  echo "前端访问：http://localhost:8000"
  echo "后端接口：http://localhost:8010/api"
  echo "接口文档：默认关闭，如需本地调试请开启 KNIFE4J_ENABLED 和 SPRINGDOC_*"
  echo "MinIO 控制台：http://localhost:9001"
}

start_local() {
  ensure_env
  cd "$DEPLOY_DIR"
  compose_cmd up -d mysql redis minio
  echo "基础服务已启动：MySQL、Redis、MinIO。"

  if ! command -v mvn >/dev/null 2>&1; then
    echo "未检测到 mvn，无法自动启动后端。请安装 Maven 后执行：cd backend && mvn spring-boot:run"
    exit 1
  fi
  if ! command -v npm >/dev/null 2>&1; then
    echo "未检测到 npm，无法自动启动前端。请安装 Node.js 后执行：cd frontend && npm install && npm run dev"
    exit 1
  fi

  echo "准备启动本地后端和前端，按 Ctrl+C 可停止。"
  (
    cd "$ROOT_DIR/backend"
    SERVER_PORT="${BACKEND_PORT:-8010}" mvn spring-boot:run
  ) &
  backend_pid=$!

  (
    cd "$ROOT_DIR/frontend"
    if [ ! -d node_modules ]; then
      npm install
    fi
    npm run dev
  ) &
  frontend_pid=$!

  trap 'kill "$backend_pid" "$frontend_pid" 2>/dev/null || true' INT TERM EXIT
  wait
}

case "$command" in
  start)
    start_all
    ;;
  stop)
    cd "$DEPLOY_DIR"
    compose_cmd down
    ;;
  restart)
    cd "$DEPLOY_DIR"
    compose_cmd down
    start_all
    ;;
  status)
    cd "$DEPLOY_DIR"
    compose_cmd ps
    ;;
  logs)
    cd "$DEPLOY_DIR"
    compose_cmd logs -f
    ;;
  build)
    ensure_env
    cd "$DEPLOY_DIR"
    compose_cmd up -d --build
    ;;
  migrate)
    migrate_db
    ;;
  local)
    start_local
    ;;
  help|-h|--help)
    print_help
    ;;
  *)
    echo "未知命令：$command"
    echo
    print_help
    exit 1
    ;;
esac

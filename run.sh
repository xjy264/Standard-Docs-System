#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$ROOT_DIR/deploy"
ENV_FILE="$DEPLOY_DIR/.env"
ENV_EXAMPLE="$DEPLOY_DIR/.env.example"
BACKUP_DIR="$ROOT_DIR/backups"

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
  ./run.sh deploy          构建并启动生产部署服务
  ./run.sh rollback        回滚到本机上一版镜像并重启
  ./run.sh doctor          执行部署自检
  ./run.sh backup          备份 MySQL 数据库，默认保留 14 天
  ./run.sh restore FILE    从备份文件恢复 MySQL 数据库
  ./run.sh migrate         对已有数据库执行幂等迁移脚本
  ./run.sh export-errors   导出最近 7 天系统错误故障包
  ./run.sh init-prod-env   生成生产部署 .env 模板和随机密钥
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

random_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -base64 36 | tr -d '\n'
    return
  fi
  date +%s%N | shasum -a 256 | awk '{print $1}'
}

init_prod_env() {
  if [ -f "$ENV_FILE" ]; then
    echo "${ENV_FILE} 已存在，为避免覆盖生产配置，本次不修改。"
    exit 0
  fi
  cat > "$ENV_FILE" <<EOF
APP_DOMAIN=standard-docs.local
APP_VERSION=0.1.0
GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo local)
BACKEND_PORT=8010
FRONTEND_PORT=8000
MYSQL_PORT=3306
REDIS_PORT=6379
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
MYSQL_DATABASE=dt_standard_system
MYSQL_USER=standard
MYSQL_PASSWORD=$(random_secret)
MYSQL_ROOT_PASSWORD=$(random_secret)
REDIS_PASSWORD=$(random_secret)
MINIO_ROOT_USER=standard-docs-minio
MINIO_ROOT_PASSWORD=$(random_secret)
MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_ENDPOINT=http://localhost:9000
ONLYOFFICE_ENABLED=false
ONLYOFFICE_URL=http://localhost:8082
ONLYOFFICE_PORT=8082
ONLYOFFICE_ALLOW_PRIVATE_IP_ADDRESS=true
JWT_SECRET=$(random_secret)$(random_secret)
AUTH_COOKIE_SECURE=false
KNIFE4J_ENABLED=false
SPRINGDOC_API_DOCS_ENABLED=false
SPRINGDOC_SWAGGER_UI_ENABLED=false
BACKUP_PASSWORD=$(random_secret)
EOF
  chmod 600 "$ENV_FILE"
  mkdir -p "$BACKUP_DIR"
  echo "已生成 ${ENV_FILE}。请按内网域名、HTTPS 入口和端口规划调整后再执行 ./run.sh deploy。"
  echo "生产环境启用 HTTPS 后请设置 AUTH_COOKIE_SECURE=true，并首次登录后立即修改默认管理员密码。"
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
  "15-error-events.sql"
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

deploy_all() {
  ensure_env
  cd "$DEPLOY_DIR"
  compose_cmd up -d --build
  echo "部署命令已执行。执行 ./run.sh doctor 可检查服务状态。"
}

rollback_all() {
  ensure_env
  cd "$DEPLOY_DIR"
  compose_cmd up -d --no-build
  echo "已按本机已有镜像重启。若需要回到指定版本，请先切换代码或镜像标签后再执行本命令。"
}

doctor() {
  ensure_env
  cd "$DEPLOY_DIR"
  echo "== Docker 服务状态 =="
  compose_cmd ps
  echo
  echo "== 数据库连接 =="
  if compose_cmd exec -T mysql sh -lc 'MYSQL_PWD="$MYSQL_PASSWORD" mysqladmin ping -h127.0.0.1 -u"$MYSQL_USER" --silent' >/dev/null 2>&1; then
    echo "MySQL: OK"
  else
    echo "MySQL: FAIL"
  fi
  echo
  echo "== Redis 连接 =="
  if compose_cmd exec -T redis sh -lc 'redis-cli -a "$REDIS_PASSWORD" ping' 2>/dev/null | grep -q PONG; then
    echo "Redis: OK"
  else
    echo "Redis: FAIL"
  fi
  echo
  echo "== 后端健康检查 =="
  local backend_port
  backend_port="$(env_value BACKEND_PORT 8010)"
  if command -v curl >/dev/null 2>&1 && curl -fsS "http://127.0.0.1:${backend_port}/actuator/health" >/dev/null 2>&1; then
    curl -fsS "http://127.0.0.1:${backend_port}/actuator/health" || true
  else
    echo "Backend health: 未能通过本机端口访问，请查看 docker compose logs backend。"
  fi
}

backup_db() {
  ensure_env
  mkdir -p "$BACKUP_DIR"
  local timestamp output encrypted password
  timestamp="$(date +%Y%m%d-%H%M%S)"
  output="$BACKUP_DIR/dt-standard-${timestamp}.sql.gz"
  cd "$DEPLOY_DIR"
  echo "开始备份数据库..."
  compose_cmd exec -T mysql sh -lc 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u"$MYSQL_USER" "$MYSQL_DATABASE"' | gzip > "$output"
  password="$(env_value BACKUP_PASSWORD)"
  if [ -n "$password" ] && command -v openssl >/dev/null 2>&1; then
    encrypted="${output}.enc"
    openssl enc -aes-256-cbc -pbkdf2 -salt -pass "pass:$password" -in "$output" -out "$encrypted"
    rm -f "$output"
    output="$encrypted"
  fi
  find "$BACKUP_DIR" -type f -name 'dt-standard-*' -mtime +14 -delete
  echo "数据库备份完成：$output"
}

restore_db() {
  ensure_env
  local file="${2:-}"
  if [ -z "$file" ] || [ ! -f "$file" ]; then
    echo "请提供要恢复的备份文件：./run.sh restore backups/xxx.sql.gz[.enc]"
    exit 1
  fi
  local input="$file" temp=""
  if [[ "$file" == *.enc ]]; then
    local password
    password="$(env_value BACKUP_PASSWORD)"
    if [ -z "$password" ]; then
      echo "恢复加密备份需要在 deploy/.env 中配置 BACKUP_PASSWORD。"
      exit 1
    fi
    temp="$(mktemp)"
    openssl enc -d -aes-256-cbc -pbkdf2 -pass "pass:$password" -in "$file" -out "$temp"
    input="$temp"
  fi
  cd "$DEPLOY_DIR"
  echo "即将恢复数据库：$file"
  gzip -dc "$input" | compose_cmd exec -T mysql sh -lc 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"'
  [ -n "$temp" ] && rm -f "$temp"
  echo "数据库恢复完成。"
}

export_errors() {
  ensure_env
  local days="${2:-7}"
  if ! [[ "$days" =~ ^[0-9]+$ ]]; then
    echo "导出天数必须是数字。"
    exit 1
  fi
  local timestamp tmp output
  timestamp="$(date +%Y%m%d-%H%M%S)"
  tmp="$(mktemp -d)"
  output="$ROOT_DIR/error-events-${timestamp}.zip"
  cd "$DEPLOY_DIR"
  echo "导出最近 ${days} 天系统错误..."
  compose_cmd exec -T mysql sh -lc "MYSQL_PWD=\"\$MYSQL_PASSWORD\" mysql -u\"\$MYSQL_USER\" \"\$MYSQL_DATABASE\" --batch --raw -e \"SELECT id,error_id,trace_id,source,severity,status_code,business_code,message,exception_class,request_uri,frontend_route,user_id,dept_id,resolved,created_at FROM sys_error_event WHERE created_at >= DATE_SUB(NOW(), INTERVAL ${days} DAY) ORDER BY created_at DESC\"" > "$tmp/error-events.tsv" || true
  compose_cmd exec -T mysql sh -lc "MYSQL_PWD=\"\$MYSQL_PASSWORD\" mysql -u\"\$MYSQL_USER\" \"\$MYSQL_DATABASE\" --batch --raw --skip-column-names -e \"SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT('id',id,'errorId',error_id,'traceId',trace_id,'source',source,'severity',severity,'message',message,'exceptionClass',exception_class,'requestUri',request_uri,'frontendRoute',frontend_route,'stackTrace',stack_trace,'frontendStack',frontend_stack,'createdAt',created_at)), JSON_ARRAY()) FROM sys_error_event WHERE created_at >= DATE_SUB(NOW(), INTERVAL ${days} DAY)\"" > "$tmp/error-events.json" || true
  compose_cmd logs --since "${days}d" backend frontend > "$tmp/docker-logs.txt" 2>/dev/null || true
  (cd "$ROOT_DIR" && ./run.sh doctor > "$tmp/doctor.txt" 2>&1) || true
  cat > "$tmp/README.txt" <<EOF
本故障包由 ./run.sh export-errors 生成。
error-events.tsv 为表格数据，error-events.json 包含完整错误堆栈，docker-logs.txt 为最近容器日志摘要。
敏感字段在应用落库前已做脱敏；请仍按内部故障资料管理。
EOF
  (cd "$tmp" && zip -q "$output" ./*)
  rm -rf "$tmp"
  echo "错误故障包已生成：$output"
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
  deploy)
    deploy_all
    ;;
  rollback)
    rollback_all
    ;;
  doctor)
    doctor
    ;;
  backup)
    backup_db
    ;;
  restore)
    restore_db "$@"
    ;;
  migrate)
    migrate_db
    ;;
  export-errors)
    export_errors "$@"
    ;;
  init-prod-env)
    init_prod_env
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

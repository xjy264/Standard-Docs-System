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

start_all() {
  ensure_env
  cd "$DEPLOY_DIR"
  compose_cmd up -d
  echo
  echo "服务启动命令已执行。"
  echo "前端访问：http://localhost:8000"
  echo "后端接口：http://localhost:8010/api"
  echo "接口文档：http://localhost:8010/doc.html"
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

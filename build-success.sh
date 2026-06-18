#!/bin/bash
# ============================================================
# 构建完成提示脚本 - small-parts-inventory
# 读取 .env 获取端口，检查端口占用，打印访问地址
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

# ---------- 读取 .env ----------
if [ -f "${ENV_FILE}" ]; then
  FRONTEND_PORT=$(grep '^FRONTEND_PORT=' "${ENV_FILE}" | cut -d'=' -f2 | tr -d '[:space:]')
  BACKEND_PORT=$(grep '^BACKEND_PORT=' "${ENV_FILE}" | cut -d'=' -f2 | tr -d '[:space:]')
  MYSQL_PORT=$(grep '^MYSQL_PORT=' "${ENV_FILE}" | cut -d'=' -f2 | tr -d '[:space:]')
  REDIS_PORT=$(grep '^REDIS_PORT=' "${ENV_FILE}" | cut -d'=' -f2 | tr -d '[:space:]')
  PROJECT_NAME=$(grep '^PROJECT_NAME=' "${ENV_FILE}" | cut -d'=' -f2 | tr -d '[:space:]')
else
  FRONTEND_PORT=3008
  BACKEND_PORT=8088
  MYSQL_PORT=3309
  REDIS_PORT=6380
  PROJECT_NAME=small-parts-inventory
fi

echo ""
echo "============================================================"
echo "   ${PROJECT_NAME} 构建启动完成"
echo "============================================================"
echo ""

# ---------- 端口占用检查 ----------
check_port() {
  local PORT=$1
  local NAME=$2
  if command -v lsof >/dev/null 2>&1; then
    PID_INFO=$(lsof -nP -iTCP:${PORT} -sTCP:LISTEN 2>/dev/null | tail -n1 | awk '{print $2, $1}')
    if [ -n "${PID_INFO}" ]; then
      echo "  ✅  ${NAME} 端口 ${PORT}  已监听 (PID ${PID_INFO})"
    else
      echo "  ⚠️   ${NAME} 端口 ${PORT}  暂无监听，请检查容器是否启动"
    fi
  else
    echo "  ℹ️   ${NAME} 端口 ${PORT}"
  fi
}

check_port "${FRONTEND_PORT}" "前端(Nginx)"
check_port "${BACKEND_PORT}" "后端(SpringBoot)"
check_port "${MYSQL_PORT}"   "MySQL"
check_port "${REDIS_PORT}"   "Redis"

echo ""
echo "============================================================"
echo "  🌐  前端访问地址（任选其一，两者等价）"
echo "     http://localhost:${FRONTEND_PORT}"
echo "     http://127.0.0.1:${FRONTEND_PORT}"
echo ""
echo "  🔧  后端 API 地址"
echo "     http://127.0.0.1:${BACKEND_PORT}/api"
echo ""
echo "  🐬  MySQL (仅本机)"
echo "     127.0.0.1:${MYSQL_PORT}"
echo ""
echo "  🟥  Redis (仅本机)"
echo "     127.0.0.1:${REDIS_PORT}"
echo "============================================================"
echo ""

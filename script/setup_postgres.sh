#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

# 设置变量
CONTAINER_NAME="agentx-postgres"
DB_NAME="agentx"
DB_USER="postgres"
DB_PASSWORD="postgres"
DB_PORT="5432"
HOST_PORT="5432"

# 获取项目根目录的绝对路径
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_DIR="$PROJECT_ROOT/docs/sql"

echo -e "${GREEN}AgentX PostgreSQL 数据库初始化脚本${NC}"
echo "==============================================="
echo "容器名称: $CONTAINER_NAME"
echo "数据库名称: $DB_NAME"
echo "数据库用户: $DB_USER"
echo "数据库端口: $DB_PORT"
echo "主机映射端口: $HOST_PORT"
echo "SQL目录路径: $SQL_DIR"
echo "==============================================="

# 检查是否安装了 Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

# 检查 SQL 目录是否存在
if [ ! -d "$SQL_DIR" ]; then
    echo -e "${RED}错误: SQL 目录 '$SQL_DIR' 不存在${NC}"
    exit 1
fi

# 检查 SQL 目录中是否包含 .sql 文件
if [ -z "$(find "$SQL_DIR" -name "*.sql" -type f)" ]; then
    echo -e "${RED}错误: SQL 目录 '$SQL_DIR' 中没有找到 .sql 文件${NC}"
    exit 1
fi

# 检查容器是否已存在
if docker ps -a | grep -q "$CONTAINER_NAME"; then
    echo -e "${YELLOW}警告: 容器 '$CONTAINER_NAME' 已存在${NC}"
    read -p "是否删除已有容器? [y/N] " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "删除已有容器..."
        docker stop "$CONTAINER_NAME" > /dev/null 2>&1
        docker rm "$CONTAINER_NAME" > /dev/null 2>&1
    else
        echo "操作取消"
        exit 0
    fi
fi

# 检查端口是否被占用
if lsof -Pi :$HOST_PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}警告: 端口 $HOST_PORT 已被占用${NC}"
    read -p "是否使用其他端口? [y/N] " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        read -p "请输入新的端口号: " HOST_PORT
    else
        echo "操作取消"
        exit 0
    fi
fi

echo -e "${GREEN}启动 PostgreSQL 容器...${NC}"
docker run --name "$CONTAINER_NAME" \
    -e POSTGRES_USER="$DB_USER" \
    -e POSTGRES_PASSWORD="$DB_PASSWORD" \
    -e POSTGRES_DB="$DB_NAME" \
    -p "$HOST_PORT:$DB_PORT" \
    -v "$SQL_DIR:/docker-entrypoint-initdb.d" \
    -d ankane/pgvector:latest

if [ $? -ne 0 ]; then
    echo -e "${RED}错误: 容器启动失败${NC}"
    exit 1
fi

echo -e "${GREEN}等待 PostgreSQL 启动...${NC}"
sleep 5

# 等待 PostgreSQL 准备就绪
RETRIES=10
until docker exec "$CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo "等待 PostgreSQL 启动中，剩余尝试次数: $RETRIES"
    RETRIES=$((RETRIES-1))
    sleep 3
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${RED}错误: PostgreSQL 启动超时${NC}"
    exit 1
fi

echo
echo -e "${GREEN}PostgreSQL 容器已成功启动！${NC}"
echo "容器名称: $CONTAINER_NAME"
echo "连接信息:"
echo "  主机: localhost"
echo "  端口: $HOST_PORT"
echo "  用户: $DB_USER"
echo "  密码: $DB_PASSWORD"
echo "  数据库: $DB_NAME"
echo "  连接URL: jdbc:postgresql://localhost:$HOST_PORT/$DB_NAME"
echo
echo "你可以使用以下命令连接到数据库:"
echo "  docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME"
echo
echo -e "${GREEN}数据库初始化完成！${NC}"

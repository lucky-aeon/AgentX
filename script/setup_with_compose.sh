#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # 无颜色

# 获取项目根目录的绝对路径
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_DIR="$PROJECT_ROOT/script"
SQL_DIR="$PROJECT_ROOT/docs/sql"

# 切换到脚本目录
cd "$SCRIPT_DIR"

echo -e "${GREEN}AgentX PostgreSQL 数据库初始化脚本 (Docker Compose 版)${NC}"
echo "==============================================="
echo "数据库名称: agentx"
echo "数据库用户: postgres"
echo "数据库密码: postgres"
echo "端口: 5432"
echo "SQL目录: $SQL_DIR"
echo "==============================================="

# 检查 Docker 和 Docker Compose 是否已安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}错误: Docker Compose 未安装，请先安装 Docker Compose${NC}"
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

# 检查 docker-compose.yml 文件是否存在
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}错误: docker-compose.yml 文件不存在${NC}"
    exit 1
fi

# 检查容器是否已存在
if docker ps -a | grep -q "agentx-postgres"; then
    echo -e "${YELLOW}警告: 容器 'agentx-postgres' 已存在${NC}"
    read -p "是否停止并删除已有容器? [y/N] " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # 尝试使用 docker-compose 停止并删除容器
        echo "停止并删除容器..."
        
        # 检查是否支持 docker compose 命令
        if docker compose version &> /dev/null; then
            docker compose down -v
        else
            docker-compose down -v
        fi
        
        # 如果 docker-compose 失败，使用 docker 命令删除
        if docker ps -a | grep -q "agentx-postgres"; then
            echo "使用 docker 命令停止容器..."
            docker stop agentx-postgres > /dev/null
            echo "删除容器..."
            docker rm agentx-postgres > /dev/null
        fi
    else
        echo "操作取消"
        exit 0
    fi
fi

echo -e "${GREEN}启动 PostgreSQL 容器...${NC}"

# 使用 docker compose 或 docker-compose 根据环境支持
if docker compose version &> /dev/null; then
    docker compose up -d
else
    docker-compose up -d
fi

# 检查容器是否成功启动
if [ $? -ne 0 ]; then
    echo -e "${RED}错误: 容器启动失败${NC}"
    exit 1
fi

echo -e "${GREEN}等待 PostgreSQL 启动...${NC}"
sleep 5

# 确认 PostgreSQL 是否已准备好接受连接
RETRIES=3
until docker exec agentx-postgres pg_isready -U postgres -d agentx > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo "等待 PostgreSQL 启动中，剩余尝试次数: $RETRIES"
    RETRIES=$((RETRIES-1))
    sleep 3
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${RED}错误: PostgreSQL 启动超时${NC}"
    exit 1
fi

echo -e "${GREEN}PostgreSQL 容器已成功启动！${NC}"
echo "容器名称: agentx-postgres"
echo "连接信息:"
echo "  主机: localhost"
echo "  端口: 5432"
echo "  用户: postgres"
echo "  密码: postgres"
echo "  数据库: agentx"
echo "  连接URL: jdbc:postgresql://localhost:5432/agentx"
echo 
echo "你可以使用以下命令连接到数据库:"
echo "  docker exec -it agentx-postgres psql -U postgres -d agentx"
echo
echo -e "${GREEN}数据库初始化完成！${NC}"

# 显示可用的命令
echo -e "${YELLOW}使用指南:${NC}"
echo "  启动容器: cd script && ./setup_with_compose.sh"
echo "  停止容器: cd script && docker-compose down 或 docker compose down"
echo "  查看容器状态: docker ps"
echo "  查看容器日志: docker logs agentx-postgres" 
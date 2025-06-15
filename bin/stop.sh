#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

# 获取项目根目录的绝对路径
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo -e "${BLUE}================================"
echo -e "      AgentX 服务停止脚本"
echo -e "================================${NC}"

# 切换到项目根目录
cd "$PROJECT_ROOT"

echo -e "${YELLOW}正在停止 AgentX 项目的所有服务...${NC}"

# 检查是否有正在运行的容器
if docker ps | grep -q "agentx-"; then
    # 使用 Docker Compose 停止服务（只停止，不删除容器）
    if docker compose version &> /dev/null; then
        docker compose stop
    else
        docker-compose stop
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}所有服务已成功停止${NC}"
        echo -e "${BLUE}提示: 容器已停止但未删除，如需删除容器请运行:${NC}"
        echo -e "${YELLOW}  docker compose down${NC}"
        echo -e "${YELLOW}  或者 docker-compose down${NC}"
    else
        echo -e "${RED}停止服务时出现错误${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}没有发现正在运行的 AgentX 服务${NC}"
fi

echo -e "${BLUE}服务状态检查:${NC}"
docker ps --filter "name=agentx-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo -e "${GREEN}✅ AgentX 项目已停止${NC}" 
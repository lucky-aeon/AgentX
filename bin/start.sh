#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

# 获取项目根目录的绝对路径
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo -e "${BLUE}"
echo "   ▄▄▄        ▄████  ▓█████  ███▄    █ ▄▄▄█████▓▒██   ██▒"
echo "  ▒████▄     ██▒ ▀█▒ ▓█   ▀  ██ ▀█   █ ▓  ██▒ ▓▒▒▒ █ █ ▒░"
echo "  ▒██  ▀█▄  ▒██░▄▄▄░ ▒███   ▓██  ▀█ ██▒▒ ▓██░ ▒░░░  █   ░"
echo "  ░██▄▄▄▄██ ░▓█  ██▓ ▒▓█  ▄ ▓██▒  ▐▌██▒░ ▓██▓ ░  ░ █ █ ▒ "
echo "   ▓█   ▓██▒░▒▓███▀▒ ░▒████▒▒██░   ▓██░  ▒██▒ ░ ▒██▒ ▒██▒"
echo -e "   ▒▒   ▓▒█░ ░▒   ▒  ░░ ▒░ ░░ ▒░   ▒ ▒   ▒ ░░   ▒▒ ░ ░▓ ░ ${NC}"
echo -e "${GREEN}              智能AI助手平台 - 一键启动脚本${NC}"
echo -e "${BLUE}========================================================${NC}"
echo
echo -e "${GREEN}项目根目录: ${PROJECT_ROOT}${NC}"
echo
echo -e "${YELLOW}包含的服务:${NC}"
echo "  - PostgreSQL 数据库 (端口: 5432)"
echo "  - API Premium Gateway (端口: 8081)"
echo "  - AgentX 后端服务 (端口: 8080)"
echo "  - AgentX 前端服务 (端口: 3000)"
echo

# 检查 Docker 和 Docker Compose 是否已安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}错误: Docker Compose 未安装，请先安装 Docker Compose${NC}"
    exit 1
fi

# 切换到项目根目录
cd "$PROJECT_ROOT"

# 检查必要的文件是否存在
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}错误: docker-compose.yml 文件不存在${NC}"
    exit 1
fi

if [ ! -f "docs/sql/init.sql" ]; then
    echo -e "${RED}错误: 数据库初始化文件 'docs/sql/init.sql' 不存在${NC}"
    exit 1
fi

# 检查数据库是否已存在
DB_EXISTS=false
if docker volume ls | grep -q "agentx-postgres-data"; then
    DB_EXISTS=true
fi

# 数据库初始化确认
if [ "$DB_EXISTS" = true ]; then
    echo -e "${YELLOW}检测到已存在的数据库数据${NC}"
    echo -e "${YELLOW}是否重新初始化数据库？这将删除所有现有数据。${NC}"
    echo -e "${RED}注意: 选择 'y' 将清空所有数据库数据！${NC}"
    read -p "重新初始化数据库? [y/N] (默认: N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}正在停止并删除现有容器和数据卷...${NC}"
        
        # 停止并删除容器
        if docker compose version &> /dev/null; then
            docker compose down -v --remove-orphans
        else
            docker-compose down -v --remove-orphans
        fi
        
        # 删除数据卷
        docker volume rm agentx-postgres-data 2>/dev/null || true
        
        echo -e "${GREEN}数据库将被重新初始化${NC}"
    else
        echo -e "${GREEN}跳过数据库初始化，使用现有数据${NC}"
    fi
else
    echo -e "${GREEN}首次启动，将自动初始化数据库${NC}"
fi

echo
echo -e "${BLUE}开始构建和启动服务...${NC}"

# 创建日志目录
mkdir -p logs/backend logs/gateway

# 构建并启动服务
if docker compose version &> /dev/null; then
    docker compose up --build -d
else
    docker-compose up --build -d
fi

# 检查启动是否成功
if [ $? -ne 0 ]; then
    echo -e "${RED}错误: 服务启动失败${NC}"
    exit 1
fi

echo
echo -e "${GREEN}正在等待服务启动...${NC}"

# 等待数据库启动
echo -e "${YELLOW}等待数据库启动...${NC}"
RETRIES=30
until docker exec agentx-postgres pg_isready -U postgres -d agentx > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    RETRIES=$((RETRIES-1))
    sleep 2
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${RED}错误: 数据库启动超时${NC}"
    exit 1
fi

echo -e "${GREEN}数据库已启动${NC}"

# 等待API网关启动
echo -e "${YELLOW}等待API网关启动...${NC}"
RETRIES=30
until curl -f http://localhost:8081/api/health > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    RETRIES=$((RETRIES-1))
    sleep 3
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${YELLOW}警告: API网关健康检查超时，但服务可能仍在启动中${NC}"
else
    echo -e "${GREEN}API网关已启动${NC}"
fi

# 等待后端服务启动
echo -e "${YELLOW}等待后端服务启动...${NC}"
RETRIES=30
until curl -f http://localhost:8080/actuator/health > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    RETRIES=$((RETRIES-1))
    sleep 3
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${YELLOW}警告: 后端服务健康检查超时，但服务可能仍在启动中${NC}"
else
    echo -e "${GREEN}后端服务已启动${NC}"
fi

# 等待前端服务启动
echo -e "${YELLOW}等待前端服务启动...${NC}"
RETRIES=20
until curl -f http://localhost:3000 > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    RETRIES=$((RETRIES-1))
    sleep 3
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${YELLOW}警告: 前端服务健康检查超时，但服务可能仍在启动中${NC}"
else
    echo -e "${GREEN}前端服务已启动${NC}"
fi

echo
echo -e "${GREEN}"
echo "🎉 ========================================================= 🎉"
echo "                    🚀 AGENTX 启动完成! 🚀                   "
echo "🎉 ========================================================= 🎉"
echo -e "${NC}"
echo
echo -e "${BLUE}服务访问地址:${NC}"
echo "  - 前端应用: http://localhost:3000"
echo "  - 后端API: http://localhost:8080"
echo "  - API网关: http://localhost:8081"
echo "  - 数据库连接: localhost:5432"
echo
echo -e "${YELLOW}🔐 默认登录账号:${NC}"
echo "┌────────────────────────────────────────┐"
echo "│  管理员账号                            │"
echo "│  邮箱: admin@agentx.ai                 │"
echo "│  密码: admin123                       │"
echo "├────────────────────────────────────────┤"
echo "│  测试账号                              │"
echo "│  邮箱: test@agentx.ai                  │"
echo "│  密码: test123                        │"
echo "└────────────────────────────────────────┘"
echo
echo -e "${RED}⚠️  重要提示:${NC}"
echo "  - 首次启动已自动创建默认账号"
echo "  - 建议登录后立即修改默认密码"
echo "  - 生产环境请删除测试账号"
echo
echo -e "${BLUE}数据库信息:${NC}"
echo "  - 主机: localhost"
echo "  - 端口: 5432"
echo "  - 数据库: agentx"
echo "  - 用户名: postgres"
echo "  - 密码: postgres"
echo
echo -e "${BLUE}管理命令:${NC}"
echo "  - 查看服务状态: docker-compose ps"
echo "  - 停止所有服务: docker-compose down"
echo "  - 查看日志: docker-compose logs -f [服务名]"
echo "  - 重启服务: docker-compose restart [服务名]"
echo
echo -e "${GREEN}🎉 AgentX 项目已成功启动！${NC}" 
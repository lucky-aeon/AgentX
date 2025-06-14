#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

# 获取项目根目录的绝对路径
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_GATEWAY_DIR="$PROJECT_ROOT/API-Premium-Gateway"

# 检查是否启用热更新模式
HOT_RELOAD=false
if [ "$1" = "--hot" ]; then
    HOT_RELOAD=true
fi

echo -e "${BLUE}"
echo "   ▄▄▄        ▄████  ▓█████  ███▄    █ ▄▄▄█████▓▒██   ██▒"
echo "  ▒████▄     ██▒ ▀█▒ ▓█   ▀  ██ ▀█   █ ▓  ██▒ ▓▒▒▒ █ █ ▒░"
echo "  ▒██  ▀█▄  ▒██░▄▄▄░ ▒███   ▓██  ▀█ ██▒▒ ▓██░ ▒░░░  █   ░"
echo "  ░██▄▄▄▄██ ░▓█  ██▓ ▒▓█  ▄ ▓██▒  ▐▌██▒░ ▓██▓ ░  ░ █ █ ▒ "
echo "   ▓█   ▓██▒░▒▓███▀▒ ░▒████▒▒██░   ▓██░  ▒██▒ ░ ▒██▒ ▒██▒"
echo -e "   ▒▒   ▓▒█░ ░▒   ▒  ░░ ▒░ ░░ ▒░   ▒ ▒   ▒ ░░   ▒▒ ░ ░▓ ░ ${NC}"
echo -e "${GREEN}            智能AI助手平台 - 开发模式智能启动${NC}"
echo -e "${BLUE}========================================================${NC}"
echo
echo -e "${GREEN}项目根目录: ${PROJECT_ROOT}${NC}"
echo
echo -e "${YELLOW}🚀 开发模式特性:${NC}"
echo "  - 智能依赖检查，首次自动构建"
echo "  - Maven/NPM 依赖缓存，加速构建"
echo "  - API网关自动克隆和构建"
echo "  - 数据库自动初始化"
echo "  - 服务健康检查"
echo "  - 🔥 支持热更新模式（实时代码更改）"
echo
echo -e "${BLUE}启动选项:${NC}"
echo "  - 默认模式: 使用依赖缓存，重启生效"
echo "  - 热更新模式: 添加 --hot 参数，代码实时生效"
echo "    用法: ./bin/start-dev.sh --hot"
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

# 检查 Git 是否已安装
if ! command -v git &> /dev/null; then
    echo -e "${RED}错误: Git 未安装，请先安装 Git${NC}"
    exit 1
fi

# 切换到项目根目录
cd "$PROJECT_ROOT"

# 检查必要的文件是否存在
COMPOSE_FILE="docker-compose.dev.yml"
if [ "$HOT_RELOAD" = true ]; then
    COMPOSE_FILE="docker-compose.hotreload.yml"
    echo -e "${GREEN}🔥 热更新模式已启用！${NC}"
fi

if [ ! -f "$COMPOSE_FILE" ]; then
    echo -e "${RED}错误: $COMPOSE_FILE 文件不存在${NC}"
    exit 1
fi

if [ ! -f "docs/sql/01_init.sql" ]; then
    echo -e "${RED}错误: 数据库初始化文件 'docs/sql/01_init.sql' 不存在${NC}"
    exit 1
fi

# 工具函数：检查镜像是否存在
check_image_exists() {
    local image_name=$1
    docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${image_name}$"
}

# 工具函数：检查容器是否运行
check_container_running() {
    local container_name=$1
    docker ps --filter "name=${container_name}" --filter "status=running" --format "{{.Names}}" | grep -q "^${container_name}$"
}

# 检查并克隆API网关项目
echo -e "${BLUE}1. 检查API网关项目...${NC}"
if [ ! -d "$API_GATEWAY_DIR" ]; then
    echo -e "${YELLOW}API网关项目不存在，正在克隆...${NC}"
    git clone https://github.com/lucky-aeon/API-Premium-Gateway.git "$API_GATEWAY_DIR"
    if [ $? -ne 0 ]; then
        echo -e "${RED}错误: API网关项目克隆失败${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ API网关项目克隆完成${NC}"
else
    echo -e "${GREEN}✅ API网关项目已存在${NC}"
    # 可选：更新API网关项目
    echo -e "${YELLOW}正在更新API网关项目...${NC}"
    cd "$API_GATEWAY_DIR"
    git pull origin main > /dev/null 2>&1 || echo -e "${YELLOW}⚠️  API网关项目更新失败，继续使用本地版本${NC}"
    cd "$PROJECT_ROOT"
fi

# 检查开发镜像是否存在
echo -e "${BLUE}2. 检查开发环境镜像...${NC}"
NEED_BUILD=false

if ! check_image_exists "agentx-backend:dev"; then
    echo -e "${YELLOW}后端开发镜像不存在${NC}"
    NEED_BUILD=true
fi

if ! check_image_exists "agentx-frontend:dev"; then
    echo -e "${YELLOW}前端开发镜像不存在${NC}"
    NEED_BUILD=true
fi

if ! check_image_exists "agentx-api-gateway:dev"; then
    echo -e "${YELLOW}API网关开发镜像不存在${NC}"
    NEED_BUILD=true
fi

# 创建必要的缓存卷
echo -e "${BLUE}3. 创建依赖缓存卷...${NC}"
docker volume create agentx-maven-cache > /dev/null 2>&1
docker volume create agentx-npm-cache > /dev/null 2>&1
echo -e "${GREEN}✅ 依赖缓存卷已就绪${NC}"

# 更新docker-compose配置文件中的API网关路径
echo -e "${BLUE}4. 更新配置文件...${NC}"
if [ -f "$COMPOSE_FILE" ]; then
    # 使用临时文件替换API网关路径
    sed "s|context: /Users/xhy/course/API-Premium-Gateway|context: ${API_GATEWAY_DIR}|g" "$COMPOSE_FILE" > "${COMPOSE_FILE}.tmp"
    mv "${COMPOSE_FILE}.tmp" "$COMPOSE_FILE"
    echo -e "${GREEN}✅ 配置文件已更新${NC}"
fi

# 检查数据库是否已存在
echo -e "${BLUE}5. 检查数据库状态...${NC}"
DB_EXISTS=false
if docker volume ls | grep -q "agentx-postgres-data"; then
    DB_EXISTS=true
fi

if [ "$DB_EXISTS" = true ]; then
    echo -e "${YELLOW}检测到已存在的数据库数据${NC}"
    echo -e "${YELLOW}是否重新初始化数据库？这将删除所有现有数据。${NC}"
    echo -e "${RED}注意: 选择 'y' 将清空所有数据库数据！${NC}"
    read -p "重新初始化数据库? [y/N] (默认: N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}正在重置数据库...${NC}"
        
        # 停止并删除容器
        if docker compose version &> /dev/null; then
            docker compose -f "$COMPOSE_FILE" down -v --remove-orphans
        else
            docker-compose -f "$COMPOSE_FILE" down -v --remove-orphans
        fi
        
        # 删除数据卷
        docker volume rm agentx-postgres-data 2>/dev/null || true
        
        echo -e "${GREEN}数据库将被重新初始化${NC}"
        NEED_BUILD=true
    else
        echo -e "${GREEN}跳过数据库初始化，使用现有数据${NC}"
    fi
else
    echo -e "${GREEN}首次启动，将自动初始化数据库${NC}"
    NEED_BUILD=true
fi

# 创建日志目录
mkdir -p logs/backend logs/gateway logs/frontend

echo
echo -e "${BLUE}6. 启动服务...${NC}"

# 根据检查结果选择启动方式
if [ "$NEED_BUILD" = true ]; then
    echo -e "${YELLOW}首次启动或需要重新构建，正在构建镜像...${NC}"
    echo -e "${YELLOW}⏳ 这可能需要几分钟时间，请耐心等待...${NC}"
    
    # 构建并启动服务
    if docker compose version &> /dev/null; then
        docker compose -f "$COMPOSE_FILE" up --build -d
    else
        docker-compose -f "$COMPOSE_FILE" up --build -d
    fi
else
    echo -e "${GREEN}使用已有镜像快速启动...${NC}"
    
    # 直接启动服务
    if docker compose version &> /dev/null; then
        docker compose -f "$COMPOSE_FILE" up -d
    else
        docker-compose -f "$COMPOSE_FILE" up -d
    fi
fi

# 检查启动是否成功
if [ $? -ne 0 ]; then
    echo -e "${RED}错误: 服务启动失败${NC}"
    exit 1
fi

echo
echo -e "${GREEN}正在等待服务启动...${NC}"

# 等待AgentX数据库启动
echo -e "${YELLOW}等待AgentX数据库启动...${NC}"
RETRIES=30
until docker exec agentx-postgres pg_isready -U postgres -d agentx > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    RETRIES=$((RETRIES-1))
    sleep 2
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${RED}AgentX数据库启动超时${NC}"
    exit 1
fi
echo -e "${GREEN}AgentX数据库已启动${NC}"

# 等待API网关数据库启动
echo -e "${YELLOW}等待API网关数据库启动...${NC}"
RETRIES=30
until docker exec api-gateway-postgres pg_isready -U gateway_user -d api_gateway > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    RETRIES=$((RETRIES-1))
    sleep 2
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${RED}API网关数据库启动超时${NC}"
    exit 1
fi
echo -e "${GREEN}API网关数据库已启动${NC}"

# 等待API网关启动
echo -e "${YELLOW}等待API网关启动...${NC}"
RETRIES=60
until curl -f http://localhost:8081/api/health > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    RETRIES=$((RETRIES-1))
    sleep 3
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${YELLOW}API网关健康检查超时，但服务可能仍在启动中${NC}"
else
    echo -e "${GREEN}API网关已启动${NC}"
fi

# 等待后端服务启动
echo -e "${YELLOW}等待后端服务启动...${NC}"
RETRIES=60
until curl -f http://localhost:8080/api/health > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    RETRIES=$((RETRIES-1))
    sleep 3
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${YELLOW}后端服务健康检查超时，但服务可能仍在启动中${NC}"
else
    echo -e "${GREEN}后端服务已启动${NC}"
fi

# 等待前端服务启动
echo -e "${YELLOW}等待前端服务启动...${NC}"
RETRIES=30
until curl -f http://localhost:3000 > /dev/null 2>&1 || [ $RETRIES -eq 0 ]; do
    echo -n "."
    RETRIES=$((RETRIES-1))
    sleep 3
done

if [ $RETRIES -eq 0 ]; then
    echo -e "${YELLOW}前端服务健康检查超时，但服务可能仍在启动中${NC}"
else
    echo -e "${GREEN}前端服务已启动${NC}"
fi

echo
echo -e "${GREEN}"
echo "🎉 ========================================================= 🎉"
echo "              🚀 AGENTX 开发环境启动完成! 🚀                 "
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
echo -e "${BLUE}🛠️ 开发模式特性:${NC}"
echo "  - ✅ Maven/NPM 依赖已缓存，重启时不会重新下载"
echo "  - ✅ API网关自动克隆和更新"
echo "  - ✅ 数据库自动初始化"
echo "  - ✅ 服务健康检查确保启动成功"
echo "  - ✅ 智能镜像检查，首次构建，后续快速启动"
echo
echo -e "${BLUE}📋 开发管理命令:${NC}"
echo "  - 查看服务状态: docker compose -f $COMPOSE_FILE ps"
echo "  - 停止所有服务: docker compose -f $COMPOSE_FILE down"
echo "  - 查看日志: docker compose -f $COMPOSE_FILE logs -f [服务名]"
if [ "$HOT_RELOAD" = true ]; then
    echo "  - 🔥 热更新模式: 代码修改自动生效，无需重启！"
else
    echo "  - 重启服务: docker compose -f $COMPOSE_FILE restart [服务名]"
fi
echo
if [ "$HOT_RELOAD" = true ]; then
    echo -e "${YELLOW}🔥 热更新特性:${NC}"
    echo "  - 前端: 修改代码后浏览器自动刷新"
    echo "  - 后端: 修改Java代码后Spring Boot自动重启"
    echo "  - 配置: 修改application.yml后自动重新加载"
    echo "  - 无需手动重启容器！"
else
    echo -e "${YELLOW}⚡ 快速重启命令:${NC}"
    echo "  - 重启后端: docker compose -f $COMPOSE_FILE restart agentx-backend"
    echo "  - 重启前端: docker compose -f $COMPOSE_FILE restart agentx-frontend"
    echo "  - 重启网关: docker compose -f $COMPOSE_FILE restart api-gateway"
fi
echo
echo -e "${RED}⚠️  重要提示:${NC}"
echo "  - 首次启动已自动创建默认账号"
echo "  - 建议登录后立即修改默认密码"
echo "  - 生产环境请删除测试账号"
echo "  - API网关项目已自动克隆到: ${API_GATEWAY_DIR}"
echo
echo -e "${GREEN}🎉 AgentX 开发环境已成功启动！${NC}" 
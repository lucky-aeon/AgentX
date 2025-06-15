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

# 开发模式默认启用热更新

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
echo "  - 🔥 支持热更新模式（文件监听+容器重启）"
echo
echo -e "${BLUE}开发模式特性:${NC}"
echo "  - 文件监听: 代码变更自动重启容器"
echo "  - 智能检测: 自动选择最佳监听方案"
echo "  - 开箱即用: 无需安装额外工具"
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
echo -e "${GREEN}🔥 开发模式（热更新已启用）${NC}"

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
echo "  - MCP网关: http://localhost:8005"
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
echo "  - 停止所有服务: ./bin/stop.sh"
echo "  - 查看日志: docker compose -f $COMPOSE_FILE logs -f [服务名]"
echo "  - 🔥 热更新: 文件变更自动重启容器，无需手动操作！"
echo
echo -e "${YELLOW}🔥 开发模式特性:${NC}"
echo "  - 监听源码文件变更并自动重启对应容器"
echo "  - 避免了类加载器问题，更稳定的开发体验"
echo "  - 按 Ctrl+C 可停止文件监听"
echo
echo -e "${RED}⚠️  重要提示:${NC}"
echo "  - 首次启动已自动创建默认账号"
echo "  - 建议登录后立即修改默认密码"
echo "  - 生产环境请删除测试账号"
echo "  - API网关项目已自动克隆到: ${API_GATEWAY_DIR}"
echo
echo -e "${GREEN}🎉 AgentX 开发环境已成功启动！${NC}"
echo
echo

# 开发模式，启动文件监听
echo -e "${BLUE}🔍 正在启动开发模式热更新...${NC}"
    
    # 检查Docker Compose版本和watch功能支持
    DOCKER_COMPOSE_VERSION=""
    WATCH_METHOD=""
    
    if docker compose version &> /dev/null; then
        DOCKER_COMPOSE_VERSION=$(docker compose version --short 2>/dev/null | head -n1)
        # 检查是否支持watch功能 (v2.22.0+)
        if [[ $(echo "$DOCKER_COMPOSE_VERSION" | sed 's/v//' | sed 's/\./\n/g' | head -n1) -ge 2 ]] && \
           [[ $(echo "$DOCKER_COMPOSE_VERSION" | sed 's/v//' | sed 's/\./\n/g' | sed -n '2p') -ge 22 ]]; then
            WATCH_METHOD="compose-watch"
            echo -e "${GREEN}✅ 使用 Docker Compose Watch 功能 (${DOCKER_COMPOSE_VERSION})${NC}"
        else
            WATCH_METHOD="polling"
            echo -e "${YELLOW}⚠️  Docker Compose 版本较旧 (${DOCKER_COMPOSE_VERSION})，使用轮询模式${NC}"
        fi
    else
        WATCH_METHOD="polling"
        echo -e "${YELLOW}⚠️  使用旧版 docker-compose，使用轮询模式${NC}"
    fi
    
    echo -e "${BLUE}📋 热更新方案: ${WATCH_METHOD}${NC}"
    echo
    
    # 检查 Docker Compose 版本
    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    else
        COMPOSE_CMD="docker-compose"
    fi
    
    echo -e "${GREEN}🚀 热更新功能已启用！${NC}"
    echo -e "${BLUE}监听目录:${NC}"
    echo -e "  - 后端: ${PROJECT_ROOT}/AgentX/src"
    echo -e "  - 前端: ${PROJECT_ROOT}/agentx-frontend-plus"  
    echo -e "  - 网关: ${PROJECT_ROOT}/API-Premium-Gateway/src"
    echo -e "${YELLOW}📝 支持的文件类型: .java .xml .properties .yml .yaml .js .jsx .ts .tsx .css .scss .json${NC}"
    echo -e "${GREEN}💡 无需安装额外工具，开箱即用！${NC}"
    echo
    echo -e "${YELLOW}⚡ 开发提示:${NC}"
    echo "  - 修改代码后容器会自动重启"
    echo "  - 前端热重载，后端智能重启" 
    echo "  - 按 Ctrl+C 停止文件监听"
    echo
    echo -e "${BLUE}========================${NC}"
    echo
    
    # 询问用户是否启动文件监听
    echo -e "${YELLOW}🔥 是否立即启动文件监听？(推荐)${NC}"
    echo -e "${BLUE}  - 启动后修改代码会自动重启容器${NC}"
    echo -e "${BLUE}  - 可随时按 Ctrl+C 停止监听${NC}"
    read -p "启动文件监听? [Y/n] (默认: Y): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Nn]$ ]]; then
        echo -e "${YELLOW}⚠️  跳过文件监听，可稍后手动重启服务${NC}"
        echo -e "${BLUE}手动重启命令:${NC}"
        echo "  - 重启后端: docker compose -f $COMPOSE_FILE restart agentx-backend"
        echo "  - 重启前端: docker compose -f $COMPOSE_FILE restart agentx-frontend"
        echo "  - 重启网关: docker compose -f $COMPOSE_FILE restart api-gateway"
        echo
        echo -e "${GREEN}✅ 开发环境已启动，服务正在运行中${NC}"
        exit 0
    fi
    
    echo -e "${GREEN}🚀 正在启动文件监听...${NC}"
    echo
    
    # 重启服务函数
    restart_service() {
        local service_name=$1
        local file_path=$2
        
        echo -e "${YELLOW}📁 文件变更: ${file_path}${NC}"
        echo -e "${BLUE}🔄 重启服务: ${service_name}${NC}"
        
        # 检查容器是否存在并运行
        if docker ps --filter "name=${service_name}" --filter "status=running" --format "{{.Names}}" | grep -q "^${service_name}$"; then
            # 重启服务
            $COMPOSE_CMD -f "$COMPOSE_FILE" restart $service_name
            
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✅ ${service_name} 重启成功${NC}"
                
                # 简单的健康检查
                case $service_name in
                    "agentx-backend")
                        echo -e "${YELLOW}⏳ 等待后端服务启动...${NC}"
                        sleep 8
                        if curl -f http://localhost:8080/api/health > /dev/null 2>&1; then
                            echo -e "${GREEN}✅ 后端服务已就绪${NC}"
                        else
                            echo -e "${YELLOW}⚠️  后端服务可能还在启动中${NC}"
                        fi
                        ;;
                    "agentx-frontend")
                        echo -e "${YELLOW}⏳ 等待前端服务启动...${NC}"
                        sleep 5
                        if curl -f http://localhost:3000 > /dev/null 2>&1; then
                            echo -e "${GREEN}✅ 前端服务已就绪${NC}"
                        else
                            echo -e "${YELLOW}⚠️  前端服务可能还在启动中${NC}"
                        fi
                        ;;
                    "api-gateway")
                        echo -e "${YELLOW}⏳ 等待API网关启动...${NC}"
                        sleep 5
                        if curl -f http://localhost:8081/api/health > /dev/null 2>&1; then
                            echo -e "${GREEN}✅ API网关已就绪${NC}"
                        else
                            echo -e "${YELLOW}⚠️  API网关可能还在启动中${NC}"
                        fi
                        ;;
                esac
            else
                echo -e "${RED}❌ ${service_name} 重启失败${NC}"
            fi
        else
            echo -e "${YELLOW}⚠️  ${service_name} 容器未运行，跳过重启${NC}"
        fi
        
        echo -e "${BLUE}------------------------${NC}"
    }
    
    # 根据文件路径判断需要重启的服务
    get_service_name() {
        local file_path=$1
        
        case $file_path in
            */AgentX/src/*)
                echo "agentx-backend"
                ;;
            */agentx-frontend-plus/*)
                echo "agentx-frontend"
                ;;
            */API-Premium-Gateway/*)
                echo "api-gateway"
                ;;
            *)
                echo ""
                ;;
        esac
    }
    
    # 清理函数
    cleanup() {
        echo -e "\n${YELLOW}🛑 停止文件监听...${NC}"
        echo -e "${GREEN}✅ 文件监听已停止${NC}"
        echo -e "${BLUE}服务依然在运行中，使用以下命令管理:${NC}"
        echo -e "  - 查看状态: docker compose -f $COMPOSE_FILE ps"
        echo -e "  - 停止服务: ./bin/stop.sh"
        exit 0
    }
    
    # 设置信号处理
    trap cleanup SIGINT SIGTERM
    
    # 开始文件监听
    cd "$PROJECT_ROOT"
    
    if [ "$WATCH_METHOD" = "compose-watch" ]; then
        # 使用 Docker Compose Watch 功能
        echo -e "${GREEN}🚀 启动 Docker Compose Watch...${NC}"
        echo -e "${BLUE}💡 从现在开始，修改代码会自动重启对应容器${NC}"
        echo -e "${YELLOW}注意: 按 Ctrl+C 可停止监听并返回命令行${NC}"
        echo
        
        # 直接使用现有的 watch 配置，但不重新构建已运行的服务
        $COMPOSE_CMD -f "$COMPOSE_FILE" -f docker-compose.watch.yml watch --no-up
        
    else
        # 使用轮询模式 - 无需额外工具
        echo -e "${GREEN}🚀 启动轮询监听 (每3秒检查一次)...${NC}"
        
        # 记录初始文件状态
        declare -A file_timestamps
        
        get_file_timestamp() {
            local file_path=$1
            if [[ -f "$file_path" ]]; then
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    # macOS
                    stat -f "%m" "$file_path" 2>/dev/null
                else
                    # Linux
                    stat -c "%Y" "$file_path" 2>/dev/null
                fi
            else
                echo "0"
            fi
        }
        
        # 初始化文件时间戳
        init_timestamps() {
            echo -e "${BLUE}📋 初始化文件监听...${NC}"
            
            # 后端文件
            while IFS= read -r -d '' file; do
                if [[ "$file" =~ \.(java|xml|properties|yml|yaml)$ ]]; then
                    file_timestamps["$file"]=$(get_file_timestamp "$file")
                fi
            done < <(find "./AgentX/src" -type f -print0 2>/dev/null)
            
            # 前端文件
            while IFS= read -r -d '' file; do
                if [[ "$file" =~ \.(js|jsx|ts|tsx|css|scss|json)$ ]]; then
                    file_timestamps["$file"]=$(get_file_timestamp "$file")
                fi
            done < <(find "./agentx-frontend-plus" -type f -print0 2>/dev/null)
            
            # 网关文件
            while IFS= read -r -d '' file; do
                if [[ "$file" =~ \.(java|xml|properties|yml|yaml)$ ]]; then
                    file_timestamps["$file"]=$(get_file_timestamp "$file")
                fi
            done < <(find "./API-Premium-Gateway/src" -type f -print0 2>/dev/null)
            
            echo -e "${GREEN}✅ 监听 ${#file_timestamps[@]} 个文件${NC}"
        }
        
        # 检查文件变化
        check_file_changes() {
            local changed_files=()
            
            # 检查所有已知文件
            for file_path in "${!file_timestamps[@]}"; do
                if [[ -f "$file_path" ]]; then
                    local current_timestamp=$(get_file_timestamp "$file_path")
                    if [[ "$current_timestamp" != "${file_timestamps[$file_path]}" ]]; then
                        changed_files+=("$file_path")
                        file_timestamps["$file_path"]="$current_timestamp"
                    fi
                fi
            done
            
            # 处理变化的文件
            for changed_file in "${changed_files[@]}"; do
                local service_name=$(get_service_name "$changed_file")
                if [[ -n "$service_name" ]]; then
                    restart_service "$service_name" "$changed_file"
                fi
            done
        }
        
        # 初始化监听
        init_timestamps
        
        # 开始轮询
                 while true; do
             sleep 3
             check_file_changes
         done
     fi
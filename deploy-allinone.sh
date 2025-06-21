#!/bin/bash

# AgentX All-in-One 一键部署脚本
# 使用单一 Docker 镜像部署完整的 AgentX 系统

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
IMAGE_NAME="ghcr.io/xhy/agentx-2:latest"
CONTAINER_NAME="agentx"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 Docker 是否安装和运行
check_docker() {
    log_info "检查 Docker 环境..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        log_info "安装地址: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker 未运行，请启动 Docker"
        exit 1
    fi
    
    log_success "Docker 环境检查通过"
}

# 检查端口占用
check_ports() {
    log_info "检查端口占用..."
    
    local ports=(3000 8080 8081)
    local occupied_ports=()
    
    for port in "${ports[@]}"; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
            occupied_ports+=($port)
        fi
    done
    
    if [ ${#occupied_ports[@]} -ne 0 ]; then
        log_warning "以下端口被占用: ${occupied_ports[*]}"
        read -p "是否继续部署? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "部署已取消"
            exit 0
        fi
    else
        log_success "端口检查通过"
    fi
}

# 停止并删除现有容器
cleanup_existing() {
    if docker ps -a --format "table {{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
        log_info "发现现有的 AgentX 容器，正在清理..."
        docker stop $CONTAINER_NAME >/dev/null 2>&1 || true
        docker rm $CONTAINER_NAME >/dev/null 2>&1 || true
        log_success "现有容器已清理"
    fi
}

# 拉取最新镜像
pull_image() {
    log_info "拉取 AgentX All-in-One 镜像..."
    docker pull $IMAGE_NAME
    log_success "镜像拉取完成"
}

# 处理配置文件
handle_config() {
    if [ -f ".env" ]; then
        log_info "发现 .env 配置文件，将使用自定义配置"
        return 0
    else
        log_info "未发现 .env 文件，将使用默认配置"
        log_warning "如需自定义配置，请下载 .env.example 并重命名为 .env："
        log_warning "curl -O https://raw.githubusercontent.com/xhy/AgentX-2/main/.env.example"
        return 1
    fi
}

# 启动容器
start_container() {
    log_info "启动 AgentX 容器..."
    
    local docker_cmd="docker run -d \
        --name $CONTAINER_NAME \
        --privileged \
        -p 3000:3000 \
        -p 8080:8080 \
        -p 8081:8081 \
        -v agentx-data:/var/lib/docker"
    
    # 如果存在 .env 文件，挂载配置目录
    if [ -f ".env" ]; then
        mkdir -p ./agentx-config
        cp .env ./agentx-config/.env
        docker_cmd="$docker_cmd -v $(pwd)/agentx-config:/agentx/config"
        log_info "使用自定义 .env 配置文件"
    fi
    
    docker_cmd="$docker_cmd $IMAGE_NAME"
    
    eval $docker_cmd
    
    log_success "容器启动完成"
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务启动..."
    
    local max_attempts=60
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:3000 >/dev/null 2>&1; then
            log_success "AgentX 服务已就绪！"
            break
        fi
        
        log_info "等待服务启动 ($attempt/$max_attempts)..."
        sleep 5
        ((attempt++))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        log_warning "服务启动超时，请检查容器日志: docker logs $CONTAINER_NAME"
    fi
}

# 显示访问信息
show_access_info() {
    echo
    log_success "🎉 AgentX 部署完成！"
    echo
    echo "📱 访问地址:"
    echo "  🌐 前端界面: http://localhost:3000"
    echo "  🔌 后端API:  http://localhost:8080"
    echo "  🚪 API网关:  http://localhost:8081"
    echo
    echo "👤 默认账户:"
    echo "  管理员:     admin@agentx.ai / admin123"
    echo "  测试用户:   test@agentx.ai / test123"
    echo
    echo "🔧 管理命令:"
    echo "  查看日志:   docker logs -f $CONTAINER_NAME"
    echo "  重启服务:   docker restart $CONTAINER_NAME"
    echo "  停止服务:   docker stop $CONTAINER_NAME"
    echo "  删除容器:   docker rm $CONTAINER_NAME"
    echo
}

# 显示帮助信息
show_help() {
    echo "AgentX All-in-One 一键部署脚本"
    echo
    echo "用法: $0 [选项]"
    echo
    echo "选项:"
    echo "  -h, --help     显示帮助信息"
    echo "  -v, --version  显示版本信息"
    echo "  --no-pull      跳过镜像拉取"
    echo "  --no-wait      跳过服务等待"
    echo
}

# 主函数
main() {
    local skip_pull=false
    local skip_wait=false
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -v|--version)
                echo "AgentX All-in-One Deploy Script v1.0.0"
                exit 0
                ;;
            --no-pull)
                skip_pull=true
                shift
                ;;
            --no-wait)
                skip_wait=true
                shift
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    echo "🚀 AgentX All-in-One 一键部署"
    echo "================================"
    
    # 执行部署步骤
    check_docker
    check_ports
    cleanup_existing
    handle_config
    
    if [ "$skip_pull" = false ]; then
        pull_image
    else
        log_info "跳过镜像拉取"
    fi
    
    start_container
    
    if [ "$skip_wait" = false ]; then
        wait_for_services
    else
        log_info "跳过服务等待"
    fi
    
    show_access_info
}

# 错误处理
trap 'log_error "部署过程中出现错误，请检查日志: docker logs $CONTAINER_NAME"; exit 1' ERR

# 运行主函数
main "$@"
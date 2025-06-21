#!/bin/bash

# AgentX 一键部署脚本
# 自动下载 docker-compose 文件并启动 AgentX 服务

set -e

# 配置
REPO_URL="https://raw.githubusercontent.com/xhy/AgentX-2/main"
COMPOSE_FILE="docker-compose.standalone.yml"
WORK_DIR="agentx-deployment"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    log_success "Docker 环境检查通过"
}

# 检查端口占用
check_ports() {
    log_info "检查端口占用..."
    
    local ports=(3000 8080 8081 5432 5433 8005)
    local occupied_ports=()
    
    for port in "${ports[@]}"; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
            occupied_ports+=($port)
        fi
    done
    
    if [ ${#occupied_ports[@]} -ne 0 ]; then
        log_warning "以下端口被占用: ${occupied_ports[*]}"
        log_warning "请确保这些端口可用，或修改 docker-compose 文件中的端口映射"
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

# 创建工作目录
create_work_dir() {
    log_info "创建工作目录..."
    
    if [ -d "$WORK_DIR" ]; then
        log_warning "目录 $WORK_DIR 已存在"
        read -p "是否删除并重新创建? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            rm -rf "$WORK_DIR"
        else
            log_info "使用现有目录"
        fi
    fi
    
    mkdir -p "$WORK_DIR"
    cd "$WORK_DIR"
    log_success "工作目录创建完成: $(pwd)"
}

# 下载配置文件
download_config() {
    log_info "下载部署配置文件..."
    
    if ! curl -fsSL "$REPO_URL/$COMPOSE_FILE" -o "$COMPOSE_FILE"; then
        log_error "下载配置文件失败"
        exit 1
    fi
    
    log_success "配置文件下载完成"
}

# 拉取镜像
pull_images() {
    log_info "拉取 Docker 镜像..."
    
    if command -v docker-compose &> /dev/null; then
        docker-compose -f "$COMPOSE_FILE" pull
    else
        docker compose -f "$COMPOSE_FILE" pull
    fi
    
    log_success "镜像拉取完成"
}

# 启动服务
start_services() {
    log_info "启动 AgentX 服务..."
    
    if command -v docker-compose &> /dev/null; then
        docker-compose -f "$COMPOSE_FILE" up -d
    else
        docker compose -f "$COMPOSE_FILE" up -d
    fi
    
    log_success "服务启动完成"
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务启动..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:3000 >/dev/null 2>&1; then
            log_success "前端服务已就绪"
            break
        fi
        
        log_info "等待服务启动 ($attempt/$max_attempts)..."
        sleep 5
        ((attempt++))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        log_warning "服务启动超时，请检查日志"
    fi
}

# 显示服务状态
show_status() {
    log_info "服务状态:"
    
    if command -v docker-compose &> /dev/null; then
        docker-compose -f "$COMPOSE_FILE" ps
    else
        docker compose -f "$COMPOSE_FILE" ps
    fi
}

# 显示访问信息
show_access_info() {
    echo
    log_success "AgentX 部署完成！"
    echo
    echo "服务访问地址:"
    echo "  🌐 前端界面: http://localhost:3000"
    echo "  🔌 后端API:  http://localhost:8080"
    echo "  🚪 API网关:  http://localhost:8081"
    echo
    echo "管理命令:"
    echo "  查看日志: docker compose -f $COMPOSE_FILE logs -f"
    echo "  停止服务: docker compose -f $COMPOSE_FILE down"
    echo "  重启服务: docker compose -f $COMPOSE_FILE restart"
    echo
    echo "默认账户:"
    echo "  管理员: admin@agentx.ai / admin123"
    echo "  测试用户: test@agentx.ai / test123"
    echo
}

# 显示帮助信息
show_help() {
    echo "AgentX 一键部署脚本"
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
                echo "AgentX Deploy Script v1.0.0"
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
    
    echo "🚀 AgentX 一键部署脚本"
    echo "================================"
    
    # 执行部署步骤
    check_docker
    check_ports
    create_work_dir
    download_config
    
    if [ "$skip_pull" = false ]; then
        pull_images
    else
        log_info "跳过镜像拉取"
    fi
    
    start_services
    
    if [ "$skip_wait" = false ]; then
        wait_for_services
    else
        log_info "跳过服务等待"
    fi
    
    show_status
    show_access_info
}

# 错误处理
trap 'log_error "部署过程中出现错误，请检查日志"; exit 1' ERR

# 运行主函数
main "$@"
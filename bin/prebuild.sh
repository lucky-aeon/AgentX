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
echo -e "       AgentX 预构建脚本"
echo -e "================================${NC}"
echo
echo -e "${GREEN}项目根目录: ${PROJECT_ROOT}${NC}"
echo
echo -e "${YELLOW}此脚本将预先下载和缓存所有依赖，加速后续启动${NC}"
echo

# 检查 Docker 是否已安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

# 切换到项目根目录
cd "$PROJECT_ROOT"

echo -e "${BLUE}1. 预构建后端 Maven 依赖...${NC}"
echo -e "${YELLOW}正在下载 Maven 依赖到缓存...${NC}"

# 创建临时的 Dockerfile 用于预构建依赖
cat > AgentX/Dockerfile.prebuild << 'EOF'
FROM maven:3.9.6-eclipse-temurin-17

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
RUN mvn dependency:resolve-sources -B -q
RUN mvn dependency:resolve -B -q

CMD ["echo", "Maven dependencies cached"]
EOF

# 构建预缓存镜像
docker build -f AgentX/Dockerfile.prebuild -t agentx-maven-cache ./AgentX

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Maven 依赖预缓存完成${NC}"
else
    echo -e "${RED}❌ Maven 依赖预缓存失败${NC}"
fi

# 清理临时文件
rm -f AgentX/Dockerfile.prebuild

echo
echo -e "${BLUE}2. 预构建前端 NPM 依赖...${NC}"
echo -e "${YELLOW}正在下载 NPM 依赖到缓存...${NC}"

# 创建临时的 Dockerfile 用于预构建 NPM 依赖
cat > agentx-frontend-plus/Dockerfile.prebuild << 'EOF'
FROM node:18-alpine

WORKDIR /build
COPY package*.json ./
RUN npm install --legacy-peer-deps

CMD ["echo", "NPM dependencies cached"]
EOF

# 构建预缓存镜像
docker build -f agentx-frontend-plus/Dockerfile.prebuild -t agentx-npm-cache ./agentx-frontend-plus

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ NPM 依赖预缓存完成${NC}"
else
    echo -e "${RED}❌ NPM 依赖预缓存失败${NC}"
fi

# 清理临时文件
rm -f agentx-frontend-plus/Dockerfile.prebuild

echo
echo -e "${BLUE}3. 创建持久化缓存卷...${NC}"

# 创建 Maven 缓存卷
docker volume create agentx-maven-cache > /dev/null 2>&1
echo -e "${GREEN}✅ Maven 缓存卷已创建${NC}"

# 创建 NPM 缓存卷
docker volume create agentx-npm-cache > /dev/null 2>&1
echo -e "${GREEN}✅ NPM 缓存卷已创建${NC}"

echo
echo -e "${GREEN}"
echo "🎉 ===================================== 🎉"
echo "           预构建完成!"
echo "🎉 ===================================== 🎉"
echo -e "${NC}"
echo
echo -e "${BLUE}缓存信息:${NC}"
echo "  - Maven 依赖缓存: agentx-maven-cache"
echo "  - NPM 依赖缓存: agentx-npm-cache"
echo "  - 预构建镜像: agentx-maven-cache, agentx-npm-cache"
echo
echo -e "${YELLOW}下次启动时将会更快！${NC}"
echo "现在可以使用以下命令快速启动:"
echo "  ./start-dev.sh    # 开发模式（推荐）"
echo "  ./start.sh        # 生产模式"
echo
echo -e "${BLUE}清理缓存命令:${NC}"
echo "  docker volume rm agentx-maven-cache agentx-npm-cache"
echo "  docker rmi agentx-maven-cache agentx-npm-cache" 
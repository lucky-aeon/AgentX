# AgentX 一体化镜像
# 包含前端、后端、数据库、消息队列、API网关的完整系统

# 第一阶段：构建后端
FROM maven:3.9.6-eclipse-temurin-17 AS backend-builder
WORKDIR /build

# 复制后端代码
COPY AgentX/pom.xml ./
RUN mvn dependency:go-offline -B
COPY AgentX/src ./src
RUN mvn clean package -DskipTests

# 第二阶段：构建前端
FROM node:18-alpine AS frontend-builder
WORKDIR /build
COPY agentx-frontend-plus/package*.json ./
RUN npm install --legacy-peer-deps
COPY agentx-frontend-plus/ .
RUN npm run build

# 第三阶段：运行时镜像 - 基于pgvector镜像
FROM pgvector/pgvector:pg15

# 安装完整运行时环境
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    rabbitmq-server \
    nginx \
    supervisor \
    curl \
    wget \
    sudo \
    nodejs \
    npm \
    && rm -rf /var/lib/apt/lists/*

USER root

# 配置RabbitMQ
RUN echo "NODENAME=rabbit@localhost" > /etc/rabbitmq/rabbitmq-env.conf

# 创建应用目录
WORKDIR /app

# 复制构建的应用
COPY --from=backend-builder /build/target/*.jar /app/backend.jar
COPY --from=frontend-builder /build/.next /app/frontend/.next
COPY --from=frontend-builder /build/package.json /app/frontend/
COPY --from=frontend-builder /build/node_modules /app/frontend/node_modules

# 复制配置文件和SQL初始化脚本
COPY AgentX/src/main/resources/application.yml /app/application.yml
COPY docs/sql/01_init.sql /app/init.sql

# API网关已移除 - 用户可选择独立部署
# 如需API网关功能，请运行：docker run -d -p 8081:8081 ghcr.io/lucky-aeon/api-premium-gateway:latest

# 配置Nginx
COPY agentx-frontend-plus/ /app/frontend-src
RUN echo 'server { \
    listen 3000; \
    location / { \
        proxy_pass http://localhost:3000; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
    } \
}' > /etc/nginx/sites-available/frontend && \
    ln -s /etc/nginx/sites-available/frontend /etc/nginx/sites-enabled/ && \
    rm /etc/nginx/sites-enabled/default

# 创建数据库初始化脚本
RUN echo '#!/bin/bash\n\
service postgresql start\n\
sleep 5\n\
sudo -u postgres psql -c "CREATE USER agentx_user WITH SUPERUSER PASSWORD '\''agentx_pass'\'';" 2>/dev/null || true\n\
sudo -u postgres createdb -O agentx_user agentx 2>/dev/null || true\n\
sudo -u postgres psql -d agentx -f /app/init.sql 2>/dev/null || true\n\
' > /app/init-db.sh && chmod +x /app/init-db.sh

# 创建supervisor目录和基础配置
RUN mkdir -p /etc/supervisor/conf.d
RUN echo '[supervisord]\n\
nodaemon=true\n\
user=root\n\
\n\
[unix_http_server]\n\
file=/tmp/supervisor.sock\n\
\n\
[supervisorctl]\n\
serverurl=unix:///tmp/supervisor.sock\n\
\n\
[rpcinterface:supervisor]\n\
supervisor.rpcinterface_factory = supervisor.rpcinterface:make_main_rpcinterface\n\
\n\
[include]\n\
files = /etc/supervisor/conf.d/*.conf\n\
' > /etc/supervisor/supervisord.conf

# 暴露端口
EXPOSE 3000 8088 5432 5672 15672

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:3000 && curl -f http://localhost:8088/api/health || exit 1

# 智能启动脚本
RUN echo '#!/bin/bash\n\
set -e\n\
\n\
echo "🚀 AgentX智能启动脚本"\n\
echo "================================"\n\
\n\
# 检测外部服务配置\n\
USE_EXTERNAL_DB=false\n\
USE_EXTERNAL_MQ=false\n\
\n\
if [ -n "$EXTERNAL_DB_HOST" ]; then\n\
    echo "🔗 检测到外部数据库配置: $EXTERNAL_DB_HOST"\n\
    USE_EXTERNAL_DB=true\n\
    export DB_HOST="$EXTERNAL_DB_HOST"\n\
else\n\
    echo "🏠 使用内置数据库服务"\n\
    export DB_HOST="localhost"\n\
fi\n\
\n\
if [ -n "$EXTERNAL_RABBITMQ_HOST" ]; then\n\
    echo "🔗 检测到外部消息队列配置: $EXTERNAL_RABBITMQ_HOST"\n\
    USE_EXTERNAL_MQ=true\n\
    export RABBITMQ_HOST="$EXTERNAL_RABBITMQ_HOST"\n\
else\n\
    echo "🏠 使用内置消息队列服务"\n\
    export RABBITMQ_HOST="localhost"\n\
fi\n\
\n\
# 动态生成supervisor配置\n\
echo "📝 生成动态服务配置..."\n\
cat > /etc/supervisor/conf.d/agentx.conf << EOF\n\
[supervisord]\n\
nodaemon=true\n\
user=root\n\
\n\
EOF\n\
\n\
# 内置数据库服务配置\n\
if [ "$USE_EXTERNAL_DB" = false ]; then\n\
    echo "✅ 启用内置PostgreSQL服务"\n\
    cat >> /etc/supervisor/conf.d/agentx.conf << EOF\n\
[program:postgresql]\n\
command=/usr/lib/postgresql/15/bin/postgres -D /var/lib/postgresql/15/main\n\
user=postgres\n\
autostart=true\n\
autorestart=true\n\
priority=10\n\
startsecs=10\n\
stopsignal=INT\n\
\n\
EOF\n\
\n\
    # 初始化数据库\n\
    echo "🗄️ 初始化内置数据库..."\n\
    # 确保数据库目录权限正确\n\
    chown -R postgres:postgres /var/lib/postgresql\n\
    if [ ! -d "/var/lib/postgresql/15/main" ]; then\n\
        sudo -u postgres pg_createcluster 15 main\n\
    fi\n\
    service postgresql start\n\
    sleep 10\n\
    /app/init-db.sh\n\
    service postgresql stop\n\
else\n\
    echo "⏭️ 跳过内置数据库服务"\n\
fi\n\
\n\
# 内置消息队列服务配置\n\
if [ "$USE_EXTERNAL_MQ" = false ]; then\n\
    echo "✅ 启用内置RabbitMQ服务"\n\
    cat >> /etc/supervisor/conf.d/agentx.conf << EOF\n\
[program:rabbitmq]\n\
command=/usr/lib/rabbitmq/bin/rabbitmq-server\n\
user=rabbitmq\n\
autostart=true\n\
autorestart=true\n\
priority=20\n\
\n\
EOF\n\
else\n\
    echo "⏭️ 跳过内置消息队列服务"\n\
fi\n\
\n\
# 后端服务配置（必需）\n\
echo "✅ 启用后端服务"\n\
cat >> /etc/supervisor/conf.d/agentx.conf << EOF\n\
[program:backend]\n\
command=java -jar /app/backend.jar --spring.profiles.active=docker\n\
directory=/app\n\
autostart=true\n\
autorestart=true\n\
priority=30\n\
environment=DB_HOST=$DB_HOST,DB_PORT=${DB_PORT:-5432},DB_NAME=${DB_NAME:-agentx},DB_USER=${DB_USER:-agentx_user},DB_PASSWORD=${DB_PASSWORD:-agentx_pass},RABBITMQ_HOST=$RABBITMQ_HOST,RABBITMQ_PORT=${RABBITMQ_PORT:-5672},RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-guest},RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest}\n\
\n\
EOF\n\
\n\
# 前端服务配置（必需）\n\
echo "✅ 启用前端服务"\n\
cat >> /etc/supervisor/conf.d/agentx.conf << EOF\n\
[program:frontend]\n\
command=npm start\n\
directory=/app/frontend\n\
autostart=true\n\
autorestart=true\n\
priority=40\n\
environment=NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL:-http://localhost:8088/api}\n\
\n\
EOF\n\
\n\
# API网关已移除 - 如需使用请独立部署\n\
echo "ℹ️ API网关未包含在此镜像中，如需使用请运行独立容器"\n\
\n\
# Nginx服务配置（必需）\n\
echo "✅ 启用Nginx服务"\n\
cat >> /etc/supervisor/conf.d/agentx.conf << EOF\n\
[program:nginx]\n\
command=/usr/sbin/nginx -g "daemon off;"\n\
autostart=true\n\
autorestart=true\n\
priority=60\n\
\n\
EOF\n\
\n\
echo "🎯 服务配置完成，启动所有服务..."\n\
exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf\n\
' > /app/start.sh && chmod +x /app/start.sh

# 启动所有服务
CMD ["/app/start.sh"]
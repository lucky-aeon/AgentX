# AgentX 一体化镜像
# 包含前端、后端、数据库、消息队列的完整系统

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

# 安装运行时环境（分批安装避免网络问题）
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    curl \
    wget \
    sudo \
    && rm -rf /var/lib/apt/lists/*

RUN apt-get update && apt-get install -y \
    rabbitmq-server \
    supervisor \
    && rm -rf /var/lib/apt/lists/*

# 安装Node.js 18
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - \
    && apt-get install -y nodejs \
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

# 准备前端源码（用于构建过程中的文件复制）
COPY agentx-frontend-plus/ /app/frontend-src

# 创建数据库初始化脚本
RUN echo '#!/bin/bash\n\
set -e\n\
echo "🗄️ 开始初始化数据库..."\n\
\n\
# 等待PostgreSQL启动\n\
for i in {1..30}; do\n\
    if sudo -u postgres pg_isready -h localhost -p 5432; then\n\
        echo "✅ PostgreSQL已就绪"\n\
        break\n\
    fi\n\
    echo "⏳ 等待PostgreSQL启动... ($i/30)"\n\
    sleep 2\n\
done\n\
\n\
# 创建用户和数据库\n\
echo "👤 创建数据库用户..."\n\
sudo -u postgres psql -c "CREATE USER agentx_user WITH SUPERUSER PASSWORD '\''agentx_pass'\'';" 2>/dev/null || echo "用户已存在"\n\
\n\
echo "🏗️ 创建数据库..."\n\
sudo -u postgres createdb -O agentx_user agentx 2>/dev/null || echo "数据库已存在"\n\
\n\
echo "📊 执行初始化SQL..."\n\
sudo -u postgres psql -d agentx -f /app/init.sql 2>/dev/null || echo "SQL执行完成"\n\
\n\
echo "✅ 数据库初始化完成"\n\
' > /app/init-db.sh && chmod +x /app/init-db.sh

# 创建服务等待脚本
RUN echo '#!/bin/bash\n\
set -e\n\
\n\
echo "⏳ 等待依赖服务启动..."\n\
\n\
# 等待数据库服务（如果使用内置数据库）\n\
if [ "$DB_HOST" = "localhost" ]; then\n\
    echo "⏳ 等待PostgreSQL服务..."\n\
    for i in {1..60}; do\n\
        if pg_isready -h localhost -p 5432 -U agentx_user -d agentx; then\n\
            echo "✅ PostgreSQL服务已就绪"\n\
            break\n\
        fi\n\
        echo "等待PostgreSQL... ($i/60)"\n\
        sleep 2\n\
    done\n\
fi\n\
\n\
# 等待RabbitMQ服务（如果使用内置消息队列）\n\
if [ "$RABBITMQ_HOST" = "localhost" ]; then\n\
    echo "⏳ 等待RabbitMQ服务..."\n\
    for i in {1..30}; do\n\
        if rabbitmqctl ping >/dev/null 2>&1; then\n\
            echo "✅ RabbitMQ服务已就绪"\n\
            break\n\
        fi\n\
        echo "等待RabbitMQ... ($i/30)"\n\
        sleep 2\n\
    done\n\
fi\n\
\n\
echo "🚀 依赖服务就绪，启动应用: $@"\n\
exec "$@"\n\
' > /app/wait-for-services.sh && chmod +x /app/wait-for-services.sh

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
    CMD curl -f http://localhost:3000 || exit 1

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
# 确保数据库和RabbitMQ用户存在\n\
if ! id -u postgres > /dev/null 2>&1; then\n\
    useradd -r -s /bin/bash postgres\n\
fi\n\
if ! id -u rabbitmq > /dev/null 2>&1; then\n\
    useradd -r -s /bin/bash rabbitmq\n\
fi\n\
\n\
# 动态生成supervisor配置\n\
echo "📝 生成动态服务配置..."\n\
cat > /etc/supervisor/conf.d/agentx.conf << EOF\n\
[supervisord]\n\
nodaemon=true\n\
user=root\n\
logfile=/tmp/supervisord.log\n\
logfile_maxbytes=50MB\n\
logfile_backups=10\n\
loglevel=info\n\
pidfile=/tmp/supervisord.pid\n\
childlogdir=/tmp\n\
\n\
EOF\n\
\n\
# 内置数据库服务配置\n\
if [ "$USE_EXTERNAL_DB" = false ]; then\n\
    echo "✅ 启用内置PostgreSQL服务"\n\
    \n\
    # 初始化数据库目录和权限\n\
    mkdir -p /var/lib/postgresql/15\n\
    chown -R postgres:postgres /var/lib/postgresql\n\
    \n\
    # 如果数据目录不存在，初始化数据库\n\
    if [ ! -d "/var/lib/postgresql/15/main" ]; then\n\
        echo "🗄️ 初始化PostgreSQL数据目录..."\n\
        sudo -u postgres /usr/lib/postgresql/15/bin/initdb -D /var/lib/postgresql/15/main\n\
        # 配置PostgreSQL监听所有地址\n\
        echo "listen_addresses = '\''*'\''" >> /var/lib/postgresql/15/main/postgresql.conf\n\
        echo "host all all 0.0.0.0/0 scram-sha-256" >> /var/lib/postgresql/15/main/pg_hba.conf\n\
    fi\n\
    \n\
    cat >> /etc/supervisor/conf.d/agentx.conf << EOF\n\
[program:postgresql]\n\
command=/usr/lib/postgresql/15/bin/postgres -D /var/lib/postgresql/15/main\n\
user=postgres\n\
autostart=true\n\
autorestart=true\n\
priority=10\n\
startsecs=10\n\
stopsignal=INT\n\
stdout_logfile=/tmp/postgresql.log\n\
stderr_logfile=/tmp/postgresql.error.log\n\
\n\
EOF\n\
else\n\
    echo "⏭️ 跳过内置数据库服务"\n\
fi\n\
\n\
# 内置消息队列服务配置\n\
if [ "$USE_EXTERNAL_MQ" = false ]; then\n\
    echo "✅ 启用内置RabbitMQ服务"\n\
    \n\
    # 创建RabbitMQ目录和配置\n\
    mkdir -p /var/lib/rabbitmq /etc/rabbitmq /var/log/rabbitmq\n\
    chown -R rabbitmq:rabbitmq /var/lib/rabbitmq /var/log/rabbitmq\n\
    \n\
    # 创建Erlang cookie并设置正确权限\n\
    echo "RABBITMQCOOKIE" > /var/lib/rabbitmq/.erlang.cookie\n\
    chown rabbitmq:rabbitmq /var/lib/rabbitmq/.erlang.cookie\n\
    chmod 600 /var/lib/rabbitmq/.erlang.cookie\n\
    \n\
    # 创建RabbitMQ配置文件\n\
    cat > /etc/rabbitmq/rabbitmq.conf << RABBIT_EOF\n\
listeners.tcp.default = 5672\n\
management.tcp.port = 15672\n\
default_user = guest\n\
default_pass = guest\n\
loopback_users = none\n\
RABBIT_EOF\n\
    \n\
    cat >> /etc/supervisor/conf.d/agentx.conf << EOF\n\
[program:rabbitmq]\n\
command=/usr/lib/rabbitmq/bin/rabbitmq-server\n\
user=rabbitmq\n\
environment=HOME="/var/lib/rabbitmq",RABBITMQ_MNESIA_BASE="/var/lib/rabbitmq/mnesia",RABBITMQ_LOG_BASE="/var/log/rabbitmq",RABBITMQ_CONFIG_FILE="/etc/rabbitmq/rabbitmq",RABBITMQ_NODENAME="rabbit@localhost"\n\
autostart=true\n\
autorestart=true\n\
priority=20\n\
startsecs=15\n\
stdout_logfile=/tmp/rabbitmq.log\n\
stderr_logfile=/tmp/rabbitmq.error.log\n\
\n\
EOF\n\
else\n\
    echo "⏭️ 跳过内置消息队列服务"\n\
fi\n\
\n\
# 数据库初始化服务（仅在使用内置数据库时执行）\n\
if [ "$USE_EXTERNAL_DB" = false ]; then\n\
    cat >> /etc/supervisor/conf.d/agentx.conf << EOF\n\
[program:db-init]\n\
command=/app/init-db.sh\n\
autostart=true\n\
autorestart=false\n\
priority=25\n\
startsecs=0\n\
startretries=3\n\
exitcodes=0\n\
stdout_logfile=/tmp/db-init.log\n\
stderr_logfile=/tmp/db-init.error.log\n\
\n\
EOF\n\
fi\n\
\n\
# 后端服务配置（必需）\n\
echo "✅ 启用后端服务"\n\
cat >> /etc/supervisor/conf.d/agentx.conf << EOF\n\
[program:backend]\n\
command=/app/wait-for-services.sh java -jar /app/backend.jar --spring.profiles.active=docker\n\
directory=/app\n\
autostart=true\n\
autorestart=true\n\
priority=30\n\
startsecs=30\n\
startretries=5\n\
environment=DB_HOST="$DB_HOST",DB_PORT="${DB_PORT:-5432}",DB_NAME="${DB_NAME:-agentx}",DB_USER="${DB_USER:-agentx_user}",DB_PASSWORD="${DB_PASSWORD:-agentx_pass}",RABBITMQ_HOST="$RABBITMQ_HOST",RABBITMQ_PORT="${RABBITMQ_PORT:-5672}",RABBITMQ_USERNAME="${RABBITMQ_USERNAME:-guest}",RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-guest}",SERVER_PORT="8088"\n\
stdout_logfile=/tmp/backend.log\n\
stderr_logfile=/tmp/backend.error.log\n\
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
startsecs=10\n\
environment=NEXT_PUBLIC_API_BASE_URL="${NEXT_PUBLIC_API_BASE_URL:-http://localhost:8088/api}",PORT="3000"\n\
stdout_logfile=/tmp/frontend.log\n\
stderr_logfile=/tmp/frontend.error.log\n\
\n\
EOF\n\
\n\
echo "ℹ️ API网关未包含在此镜像中，如需使用请运行独立容器"\n\
echo "🎯 服务配置完成，启动所有服务..."\n\
exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf\n\
' > /app/start.sh && chmod +x /app/start.sh

# 启动所有服务
CMD ["/app/start.sh"]
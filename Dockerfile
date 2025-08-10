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

# 安装其他系统依赖
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    rabbitmq-server \
    nginx \
    supervisor \
    curl \
    wget \
    sudo \
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

# 下载并配置API网关
RUN mkdir -p /app/gateway && \
    cd /app && \
    curl -L -o gateway.jar https://github.com/lucky-aeon/API-Premium-Gateway/releases/latest/download/api-premium-gateway.jar || \
    echo "Warning: Could not download API gateway, will skip gateway service"

# 配置Nginx
COPY agentx-frontend-plus/ /app/frontend-src
RUN echo 'server { \
    listen 3000; \
    location / { \
        proxy_pass http://localhost:3001; \
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
sudo -u postgres psql -c "CREATE USER gateway_user WITH SUPERUSER PASSWORD '\''gateway_pass'\'';" 2>/dev/null || true\n\
sudo -u postgres createdb -O gateway_user api_gateway 2>/dev/null || true\n\
' > /app/init-db.sh && chmod +x /app/init-db.sh

# 创建supervisor配置
RUN echo '[supervisord]\n\
nodaemon=true\n\
user=root\n\
\n\
[program:postgresql]\n\
command=/usr/lib/postgresql/15/bin/postgres -D /var/lib/postgresql/15/main\n\
user=postgres\n\
autostart=true\n\
autorestart=true\n\
priority=10\n\
startsecs=10\n\
stopsignal=INT\n\
\n\
[program:rabbitmq]\n\
command=/usr/lib/rabbitmq/bin/rabbitmq-server\n\
user=rabbitmq\n\
autostart=true\n\
autorestart=true\n\
priority=20\n\
\n\
[program:backend]\n\
command=java -jar /app/backend.jar --spring.profiles.active=docker\n\
directory=/app\n\
autostart=true\n\
autorestart=true\n\
priority=30\n\
environment=DB_HOST=localhost,DB_PORT=5432,DB_NAME=agentx,DB_USER=agentx_user,DB_PASSWORD=agentx_pass\n\
\n\
[program:frontend]\n\
command=npm start\n\
directory=/app/frontend\n\
autostart=true\n\
autorestart=true\n\
priority=40\n\
environment=NEXT_PUBLIC_API_BASE_URL=http://localhost:8088/api\n\
\n\
[program:gateway]\n\
command=java -jar /app/gateway.jar --spring.profiles.active=docker\n\
directory=/app\n\
autostart=true\n\
autorestart=true\n\
priority=50\n\
environment=DB_HOST=localhost,DB_PORT=5432,DB_NAME=api_gateway,DB_USER=gateway_user,DB_PASSWORD=gateway_pass\n\
\n\
[program:nginx]\n\
command=/usr/sbin/nginx -g "daemon off;"\n\
autostart=true\n\
autorestart=true\n\
priority=60\n\
' > /etc/supervisor/conf.d/agentx.conf

# 暴露端口
EXPOSE 3000 8088 8081 5432 5672 15672 8082

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:3000 && curl -f http://localhost:8088/api/health || exit 1

# 启动脚本
RUN echo '#!/bin/bash\n\
# 创建PostgreSQL cluster（如果不存在）\n\
if [ ! -d "/var/lib/postgresql/15/main" ]; then\n\
    sudo -u postgres pg_createcluster 15 main\n\
fi\n\
\n\
# 启动PostgreSQL并初始化数据库\n\
service postgresql start\n\
sleep 10\n\
/app/init-db.sh\n\
service postgresql stop\n\
\n\
# 启动supervisor管理所有服务\n\
exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf\n\
' > /app/start.sh && chmod +x /app/start.sh

# 启动所有服务
CMD ["/app/start.sh"]
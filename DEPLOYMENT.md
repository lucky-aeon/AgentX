# AgentX Docker 部署指南

## 快速部署

用户可以直接使用预构建的 Docker 镜像部署 AgentX，无需克隆源码。

### 方式一：一键部署脚本（推荐）

```bash
# 下载并运行一键部署脚本
curl -fsSL https://raw.githubusercontent.com/xhy/AgentX-2/main/deploy.sh | bash
```

### 方式二：Docker Compose 部署

1. **下载 docker-compose 文件**
```bash
curl -O https://raw.githubusercontent.com/xhy/AgentX-2/main/docker-compose.standalone.yml
```

2. **启动服务**
```bash
docker compose -f docker-compose.standalone.yml up -d
```

### 方式三：直接拉取镜像运行

```bash
# 拉取镜像
docker pull ghcr.io/xhy/agentx-2/frontend:latest
docker pull ghcr.io/xhy/agentx-2/backend:latest  
docker pull ghcr.io/xhy/agentx-2/api-gateway:latest

# 创建网络
docker network create agentx-network

# 启动数据库
docker run -d --name agentx-postgres \
  --network agentx-network \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=agentx \
  -p 5432:5432 \
  ankane/pgvector:latest

docker run -d --name api-gateway-postgres \
  --network agentx-network \
  -e POSTGRES_DB=api_gateway \
  -e POSTGRES_USER=gateway_user \
  -e POSTGRES_PASSWORD=gateway_pass \
  -p 5433:5432 \
  postgres:15-alpine

# 启动 MCP 网关
docker run -d --name agentx-mcp-gateway \
  --network agentx-network \
  -p 8005:8080 \
  ghcr.io/lucky-aeon/mcp-gateway:latest

# 启动 API 网关
docker run -d --name agentx-api-gateway \
  --network agentx-network \
  -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://api-gateway-postgres:5432/api_gateway \
  -e SPRING_DATASOURCE_USERNAME=gateway_user \
  -e SPRING_DATASOURCE_PASSWORD=gateway_pass \
  ghcr.io/xhy/agentx-2/api-gateway:latest

# 启动后端服务
docker run -d --name agentx-backend \
  --network agentx-network \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://agentx-postgres:5432/agentx \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e MCP_GATEWAY_URL=http://agentx-mcp-gateway:8080 \
  -e HIGH_AVAILABILITY_GATEWAY_URL=http://agentx-api-gateway:8081 \
  ghcr.io/xhy/agentx-2/backend:latest

# 启动前端服务
docker run -d --name agentx-frontend \
  --network agentx-network \
  -p 3000:3000 \
  -e NODE_ENV=production \
  -e NEXT_PUBLIC_API_URL=http://localhost:8080 \
  -e NEXT_PUBLIC_GATEWAY_URL=http://localhost:8081 \
  ghcr.io/xhy/agentx-2/frontend:latest
```

### 访问服务
- 前端界面: http://localhost:3000
- 后端API: http://localhost:8080
- API网关: http://localhost:8081

## 镜像列表

AgentX 提供以下预构建镜像：

- `ghcr.io/xhy/agentx-2/frontend:latest` - 前端服务（Next.js）
- `ghcr.io/xhy/agentx-2/backend:latest` - 后端服务（Spring Boot）
- `ghcr.io/xhy/agentx-2/api-gateway:latest` - API网关服务

## 版本管理

### 镜像标签

- `latest` - 最新稳定版本
- `v1.0.0` - 具体版本号（语义化版本）
- `v1.0` - 主要版本号

### 使用特定版本

```yaml
services:
  agentx-backend:
    image: ghcr.io/xhy/agentx-2/backend:v1.0.0
```

## 环境配置

### 环境变量

| 变量名 | 服务 | 默认值 | 说明 |
|--------|------|--------|------|
| `POSTGRES_PASSWORD` | postgres | postgres | 数据库密码 |
| `NEXT_PUBLIC_API_URL` | frontend | http://localhost:8080 | 后端API地址 |
| `SPRING_PROFILES_ACTIVE` | backend/gateway | docker | Spring配置文件 |

### 端口映射

| 服务 | 内部端口 | 外部端口 | 说明 |
|------|----------|----------|------|
| frontend | 3000 | 3000 | 前端界面 |
| backend | 8080 | 8080 | 后端API |
| api-gateway | 8081 | 8081 | API网关 |
| postgres | 5432 | 5432 | 主数据库 |
| gateway-postgres | 5432 | 5433 | 网关数据库 |
| mcp-gateway | 8080 | 8005 | MCP网关 |

## 管理命令

### 查看服务状态
```bash
docker compose -f docker-compose.standalone.yml ps
```

### 查看日志
```bash
# 查看所有服务日志
docker compose -f docker-compose.standalone.yml logs -f

# 查看特定服务日志
docker compose -f docker-compose.standalone.yml logs -f agentx-backend
```

### 停止服务

**Docker Compose 方式：**
```bash
docker compose -f docker-compose.standalone.yml down
```

**直接运行方式：**
```bash
# 停止所有容器
docker stop agentx-frontend agentx-backend agentx-api-gateway agentx-mcp-gateway agentx-postgres api-gateway-postgres

# 删除所有容器
docker rm agentx-frontend agentx-backend agentx-api-gateway agentx-mcp-gateway agentx-postgres api-gateway-postgres

# 删除网络
docker network rm agentx-network
```

### 更新镜像

**Docker Compose 方式：**
```bash
# 拉取最新镜像
docker compose -f docker-compose.standalone.yml pull

# 重新启动服务
docker compose -f docker-compose.standalone.yml up -d
```

**直接运行方式：**
```bash
# 拉取最新镜像
docker pull ghcr.io/xhy/agentx-2/frontend:latest
docker pull ghcr.io/xhy/agentx-2/backend:latest
docker pull ghcr.io/xhy/agentx-2/api-gateway:latest

# 停止和删除旧容器
docker stop agentx-frontend agentx-backend agentx-api-gateway
docker rm agentx-frontend agentx-backend agentx-api-gateway

# 重新启动容器（使用上面的启动命令）
```

### 完全清理
```bash
# 停止服务并删除数据卷
docker compose -f docker-compose.standalone.yml down -v

# 删除相关镜像
docker rmi ghcr.io/xhy/agentx-2/frontend:latest
docker rmi ghcr.io/xhy/agentx-2/backend:latest
docker rmi ghcr.io/xhy/agentx-2/api-gateway:latest
```

## 故障排除

### 常见问题

1. **端口冲突**
   - 修改 docker-compose.standalone.yml 中的端口映射
   - 确保端口 3000, 8080, 8081, 5432, 5433, 8005 未被占用

2. **镜像拉取失败**
   ```bash
   # 手动拉取镜像
   docker pull ghcr.io/xhy/agentx-2/frontend:latest
   docker pull ghcr.io/xhy/agentx-2/backend:latest
   docker pull ghcr.io/xhy/agentx-2/api-gateway:latest
   ```

3. **数据库连接问题**
   - 检查数据库服务是否健康：`docker compose ps`
   - 查看数据库日志：`docker compose logs postgres`

4. **服务启动顺序**
   - 服务有依赖关系，需要按顺序启动
   - 使用 `depends_on` 和 `healthcheck` 确保启动顺序

### 日志分析

```bash
# 查看服务启动日志
docker compose -f docker-compose.standalone.yml logs --since=10m

# 查看特定时间段的日志
docker compose -f docker-compose.standalone.yml logs --since="2023-01-01T00:00:00" --until="2023-01-01T12:00:00"
```

## 生产环境建议

1. **资源限制**
   - 为每个服务设置内存和CPU限制
   - 使用 `deploy.resources` 配置

2. **数据持久化**
   - 定期备份数据库
   - 配置外部存储卷

3. **安全配置**
   - 修改默认密码
   - 配置防火墙规则
   - 使用HTTPS

4. **监控告警**
   - 集成监控系统
   - 配置健康检查告警

## 开发者信息

- 源码仓库: https://github.com/xhy/AgentX-2
- 镜像仓库: https://github.com/xhy/AgentX-2/pkgs/container
- 问题反馈: https://github.com/xhy/AgentX-2/issues
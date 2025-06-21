# AgentX Docker 部署指南

## 🚀 一键部署（推荐）

### 方式一：使用默认配置（最简单）

```bash
# 一键部署脚本 - 使用默认配置
curl -fsSL https://raw.githubusercontent.com/xhy/AgentX-2/main/deploy-allinone.sh | bash
```

### 方式二：使用自定义配置（推荐）

```bash
# 1. 下载配置文件模板
curl -O https://raw.githubusercontent.com/xhy/AgentX-2/main/.env.example

# 2. 重命名并编辑配置文件
mv .env.example .env
nano .env  # 或使用其他编辑器编辑配置

# 3. 运行部署脚本
curl -fsSL https://raw.githubusercontent.com/xhy/AgentX-2/main/deploy-allinone.sh | bash
```

### 方式三：直接运行 Docker

**使用默认配置：**
```bash
docker run -d \
  --name agentx \
  --privileged \
  -p 3000:3000 \
  -p 8080:8080 \
  -p 8081:8081 \
  -v agentx-data:/var/lib/docker \
  ghcr.io/lucky-aeon/agentx:latest
```

**使用自定义配置：**
```bash
# 创建配置目录并放入 .env 文件
mkdir -p ./agentx-config
cp .env ./agentx-config/

# 启动容器，挂载配置文件
docker run -d \
  --name agentx \
  --privileged \
  -p 3000:3000 \
  -p 8080:8080 \
  -p 8081:8081 \
  -v agentx-data:/var/lib/docker \
  -v $(pwd)/agentx-config:/agentx/config \
  ghcr.io/lucky-aeon/agentx:latest
```

### 快速访问
部署完成后直接访问：
- 🌐 **前端界面**: http://localhost:3000
- 🔌 **后端API**: http://localhost:8080  
- 🚪 **API网关**: http://localhost:8081

### 默认账户
- 👤 **管理员**: admin@agentx.ai / admin123
- 👤 **测试用户**: test@agentx.ai / test123

---

## ⚙️ 配置说明

### 配置文件结构

AgentX 使用 `.env` 文件进行配置，包含以下主要配置项：

| 配置分类 | 配置项 | 说明 | 是否必需 |
|---------|--------|------|----------|
| **数据库** | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL 数据库连接信息 | ✅ |
| **邮箱** | `MAIL_SMTP_HOST`, `MAIL_SMTP_PORT`, `MAIL_SMTP_USERNAME`, `MAIL_SMTP_PASSWORD` | SMTP 邮箱配置，用于发送验证码 | ❌ |
| **GitHub OAuth** | `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GITHUB_REDIRECT_URI` | GitHub 第三方登录 | ❌ |
| **GitHub 插件** | `GITHUB_TARGET_USERNAME`, `GITHUB_TARGET_REPO_NAME`, `GITHUB_TARGET_TOKEN` | 插件市场仓库配置 | ❌ |
| **对象存储** | `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET_NAME` 等 | 文件存储配置（支持 S3/OSS） | ❌ |
| **SSO 登录** | `SSO_COMMUNITY_APP_KEY`, `SSO_COMMUNITY_APP_SECRET` 等 | 单点登录配置 | ❌ |

### 快速配置示例

**最小配置（仅修改数据库密码）：**
```bash
# 下载配置模板
curl -O https://raw.githubusercontent.com/xhy/AgentX-2/main/.env.example
mv .env.example .env

# 编辑配置文件，只修改数据库密码
sed -i 's/DB_PASSWORD=postgres/DB_PASSWORD=your_secure_password/' .env
```

**GitHub OAuth 配置：**
```bash
# 在 .env 文件中设置 GitHub OAuth
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_REDIRECT_URI=http://localhost:3000/oauth/github/callback
```

**邮箱配置（QQ 邮箱示例）：**
```bash
# 在 .env 文件中设置邮箱
MAIL_SMTP_HOST=smtp.qq.com
MAIL_SMTP_PORT=587
MAIL_SMTP_USERNAME=your-email@qq.com
MAIL_SMTP_PASSWORD=your-email-app-password
```

### 配置文件获取

```bash
# 方式一：直接下载
curl -O https://raw.githubusercontent.com/xhy/AgentX-2/main/.env.example

# 方式二：从容器中获取
docker run --rm ghcr.io/lucky-aeon/agentx:latest cat /agentx/.env.example > .env.example
```

---

## 📋 其他部署方式

### 方式一：一键部署脚本

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

### 方式三：手动分离部署

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

## 📦 镜像架构说明

### 🏗️ 项目架构
AgentX 采用微服务架构，由多个独立仓库组成：

| 服务 | 仓库 | 镜像 | 职责 |
|------|------|------|------|
| **前端+后端** | 本仓库 (AgentX-2) | `ghcr.io/lucky-aeon/agentx:latest` | 核心业务逻辑 |
| **MCP 网关** | 外部仓库 | `ghcr.io/lucky-aeon/mcp-gateway:latest` | MCP 协议网关 |
| **数据库** | 官方镜像 | `ankane/pgvector:latest` + `postgres:15-alpine` | 数据存储 |

### 🎯 部署策略
- **一个 tag** → 触发本仓库构建 → 生成 `ghcr.io/lucky-aeon/agentx:latest`
- **All-in-One 容器** → 自动拉取所有依赖镜像 → 完整系统部署
- **用户体验** → 一条命令部署整个 AgentX 系统

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

## 🔧 管理命令

### All-in-One 镜像管理

**查看容器状态：**
```bash
docker ps | grep agentx
```

**查看日志：**
```bash
# 查看容器日志
docker logs -f agentx

# 进入容器查看服务状态
docker exec -it agentx docker compose ps
```

**重启服务：**
```bash
docker restart agentx
```

**停止服务：**
```bash
docker stop agentx
docker rm agentx
```

### Docker Compose 管理

**查看服务状态：**
```bash
docker compose -f docker-compose.standalone.yml ps
```

**查看日志：**
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
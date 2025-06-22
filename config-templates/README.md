# AgentX 配置文件使用指南

## 📋 配置文件说明

AgentX 支持通过配置文件挂载的方式进行灵活部署，容器会自动检测配置文件并切换相应的运行模式。

### 🎯 智能模式切换

- **外部数据库模式**：当配置文件中设置了 `DB_HOST`（且不为localhost）时，容器将：
  - ✅ 启动前端和后端服务
  - ❌ 跳过内置PostgreSQL数据库
  - 🔗 连接到指定的外部数据库

- **内置数据库模式**：当未配置 `DB_HOST` 或为localhost时，容器将：
  - ✅ 启动前端、后端和PostgreSQL服务
  - 🔧 自动初始化数据库和表结构
  - 🏠 使用容器内置数据库

## 📁 配置文件模板

### 1. production.env - 生产环境模板
适用于生产环境，使用外部数据库，优化的日志级别和安全配置。

### 2. development.env - 开发环境模板  
适用于开发环境，使用内置数据库，详细的调试日志。

### 3. external-database.env - 外部数据库示例
完整的外部数据库配置示例，包含数据库初始化说明。

## 🚀 部署方式

### 方式1：直接挂载配置文件

```bash
# 1. 复制并编辑配置文件
cp config-templates/production.env ./agentx.env
vim ./agentx.env

# 2. 启动容器
docker run -d \
  --name agentx-production \
  -p 3000:3000 \
  -p 8088:8088 \
  -v $(pwd)/agentx.env:/app/config/agentx.env:ro \
  agentx-production:latest
```

### 方式2：挂载配置目录

```bash  
# 1. 创建配置目录
mkdir -p ./agentx-config

# 2. 复制配置文件
cp config-templates/production.env ./agentx-config/agentx.env

# 3. 编辑配置
vim ./agentx-config/agentx.env

# 4. 启动容器
docker run -d \
  --name agentx-production \
  -p 3000:3000 \
  -p 8088:8088 \
  -v $(pwd)/agentx-config:/app/config:ro \
  agentx-production:latest
```

### 方式3：Docker Compose

```bash
# 1. 复制Docker Compose文件
cp config-templates/docker-compose.yml ./

# 2. 创建并配置配置文件
mkdir -p ./agentx-config
cp config-templates/production.env ./agentx-config/agentx.env
vim ./agentx-config/agentx.env

# 3. 启动服务
docker-compose up -d

# 4. 查看日志
docker-compose logs -f agentx
```

## ⚙️ 配置参数说明

### 数据库配置
- `DB_HOST`: 数据库主机地址（设置此项启用外部数据库模式）
- `DB_PORT`: 数据库端口（默认：5432）
- `DB_NAME`: 数据库名称（默认：agentx）
- `DB_USER`: 数据库用户（默认：agentx_user）
- `DB_PASSWORD`: 数据库密码

### 应用配置
- `SERVER_PORT`: 后端服务端口（默认：8088）
- `JPA_DDL_AUTO`: JPA DDL模式（update/validate/create）
- `SHOW_SQL`: 是否显示SQL日志（true/false）

### 日志配置
- `LOG_LEVEL_ROOT`: 根日志级别（debug/info/warn/error）
- `LOG_LEVEL_APP`: 应用日志级别（debug/info/warn/error）

### MCP网关配置
- `MCP_API_KEY`: MCP网关API密钥
- `MCP_BASE_URL`: MCP网关基础URL

### 高可用配置
- `HIGH_AVAILABILITY_ENABLED`: 是否启用高可用（true/false）
- `HIGH_AVAILABILITY_GATEWAY_URL`: 高可用网关URL

## 🔧 外部数据库初始化

如果使用外部数据库，需要手动初始化：

```bash
# 1. 连接到PostgreSQL并创建数据库
sudo -u postgres psql

# 2. 执行初始化命令
CREATE DATABASE agentx;
CREATE USER agentx_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE agentx TO agentx_user;
\q

# 3. 执行表结构初始化
psql -h your-db-host -U agentx_user -d agentx -f docs/sql/01_init.sql
```

## 📊 容器启动日志示例

### 外部数据库模式日志：
```
🚀 Starting AgentX All-in-One System
=====================================
📄 Loading configuration from /app/config/agentx.env
✅ Configuration loaded successfully
📊 Current configuration:
   DB_HOST: 192.168.1.100
   DB_PORT: 5432
   DB_NAME: agentx
   DB_USER: agentx_user
   SERVER_PORT: 8088
   JPA_DDL_AUTO: update
   LOG_LEVEL_ROOT: info

🔗 External database mode: 192.168.1.100:5432
📦 Skipping internal PostgreSQL initialization
🎯 Starting services with supervisor...
📊 Configuration mode: External Database
```

### 内置数据库模式日志：
```
🚀 Starting AgentX All-in-One System
=====================================
📄 No configuration file found at /app/config/, using default settings
📊 Current configuration:
   DB_HOST: localhost
   DB_PORT: 5432
   DB_NAME: agentx
   DB_USER: agentx_user
   SERVER_PORT: 8088
   JPA_DDL_AUTO: update
   LOG_LEVEL_ROOT: info

🏠 Internal database mode
🔧 Initializing PostgreSQL database...
🔧 Creating tables in agentx database...
🎯 Starting services with supervisor...
📊 Configuration mode: Internal Database
```

## 🔍 故障排除

### 常见问题

1. **配置文件未加载**
   - 检查配置文件路径：`/app/config/agentx.env`
   - 确认文件权限：配置文件需要容器可读

2. **外部数据库连接失败**
   - 检查数据库服务器是否运行
   - 验证网络连通性
   - 确认数据库用户权限

3. **服务启动失败**
   - 查看容器日志：`docker logs container-name`
   - 检查端口占用情况
   - 验证配置文件语法

### 调试命令

```bash
# 查看容器日志
docker logs agentx-production -f

# 进入容器检查配置
docker exec -it agentx-production bash
cat /app/config/agentx.env

# 检查服务状态
docker exec agentx-production ps aux

# 测试API连接
curl http://localhost:8088/api/health
```
# AgentX 启动脚本使用指南

## 概述

AgentX 提供了两个智能启动脚本，支持一键启动完整的微服务架构：

- **`start-dev.sh`**: 开发模式，智能依赖检查，快速迭代
- **`start.sh`**: 生产模式，完整构建，稳定运行

## 前置要求

- Docker (已安装并启动)
- Docker Compose 
- Git (用于自动克隆API网关项目)

## 快速开始

### 开发模式 (推荐)

```bash
# 一键启动开发环境
./bin/start-dev.sh
```

**开发模式特性：**
- ✅ 智能依赖检查：首次自动构建，后续使用缓存
- ✅ API网关自动克隆和更新
- ✅ Maven/NPM 依赖缓存，避免重复下载
- ✅ 数据库自动初始化
- ✅ 完整的服务健康检查

### 生产模式

```bash
# 一键启动生产环境
./bin/start.sh
```

**生产模式特性：**
- 🚀 完整镜像构建，确保最新代码
- 🔒 生产级配置
- 📊 服务健康监控

## 服务架构

启动后的服务包括：

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端应用 | 3000 | Next.js 用户界面 |
| 后端API | 8080 | Spring Boot 核心服务 |
| API网关 | 8081 | 高可用网关服务 |
| PostgreSQL | 5432 | 主数据库 |

## 项目结构

```
AgentX/
├── bin/                          # 启动脚本
│   ├── start-dev.sh             # 开发模式启动
│   └── start.sh                 # 生产模式启动
├── AgentX/                      # 后端服务源码
├── agentx-frontend-plus/        # 前端服务源码
├── API-Premium-Gateway/         # API网关 (自动克隆)
├── docs/sql/                    # 数据库初始化脚本
├── docker-compose.yml           # 生产环境配置
└── docker-compose.dev.yml       # 开发环境配置
```

## 常用命令

### 服务管理

```bash
# 查看服务状态
docker compose -f docker-compose.dev.yml ps

# 停止所有服务
docker compose -f docker-compose.dev.yml down

# 查看服务日志
docker compose -f docker-compose.dev.yml logs -f [服务名]

# 重启特定服务
docker compose -f docker-compose.dev.yml restart agentx-backend
```

### 快速重启命令

```bash
# 重启后端服务 (代码修改后)
docker compose -f docker-compose.dev.yml restart agentx-backend

# 重启前端服务
docker compose -f docker-compose.dev.yml restart agentx-frontend

# 重启API网关
docker compose -f docker-compose.dev.yml restart api-gateway
```

## 默认账号

系统启动后自动创建以下测试账号：

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 管理员 | admin@agentx.ai | admin123 |
| 测试用户 | test@agentx.ai | test123 |

⚠️ **重要**: 生产环境请立即修改默认密码并删除测试账号！

## 故障排查

### 常见问题

1. **端口被占用**
   ```bash
   # 检查端口占用
   lsof -i :3000,8080,8081,5432
   
   # 停止占用服务
   docker compose -f docker-compose.dev.yml down
   ```

2. **镜像构建失败**
   ```bash
   # 清理所有容器和镜像，重新开始
   docker compose -f docker-compose.dev.yml down -v --remove-orphans
   docker system prune -f
   
   # 重新启动
   ./bin/start-dev.sh
   ```

3. **API网关克隆失败**
   ```bash
   # 手动克隆API网关项目
   git clone https://github.com/lucky-aeon/API-Premium-Gateway.git
   ```

4. **数据库连接问题**
   ```bash
   # 重置数据库
   ./bin/start-dev.sh
   # 选择 'y' 重新初始化数据库
   ```

### 日志查看

```bash
# 查看所有服务日志
docker compose -f docker-compose.dev.yml logs -f

# 查看特定服务日志
docker compose -f docker-compose.dev.yml logs -f agentx-backend
docker compose -f docker-compose.dev.yml logs -f agentx-frontend
docker compose -f docker-compose.dev.yml logs -f api-gateway
docker compose -f docker-compose.dev.yml logs -f postgres
```

## 开发工作流

### 推荐的开发流程

1. **首次启动**
   ```bash
   git clone <项目地址>
   cd AgentX
   ./bin/start-dev.sh
   ```

2. **日常开发**
   ```bash
   # 修改代码后重启相关服务
   docker compose -f docker-compose.dev.yml restart agentx-backend
   ```

3. **测试新功能**
   ```bash
   # 完全重启环境
   ./bin/start-dev.sh
   ```

### 性能优化

- 首次启动需要下载依赖，耗时较长（约5-10分钟）
- 后续启动使用缓存，速度很快（约1-2分钟）
- 代码修改后只需重启对应服务，无需重新构建

## 技术说明

### 依赖缓存机制

- **Maven缓存**: 通过 `agentx-maven-cache` volume 持久化
- **NPM缓存**: 通过 `agentx-npm-cache` volume 持久化
- **镜像缓存**: 智能检查镜像存在性，避免重复构建

### 配置文件动态更新

启动脚本会自动：
- 克隆API网关项目到正确位置
- 更新docker-compose文件中的路径引用
- 确保所有服务使用正确的配置

## 支持

如果遇到问题：

1. 查看本文档的故障排查部分
2. 检查服务日志：`docker compose -f docker-compose.dev.yml logs -f`
3. 提交Issue并附上错误日志

---

**Happy Coding! 🚀** 
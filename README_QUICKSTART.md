# AgentX 一键启动指南

AgentX 项目提供了完整的一键启动方案，支持在容器环境中运行所有服务，无需本地环境依赖。

## 🚀 快速启动

### 前置要求

- Docker (版本 20.10 或更高)
- Docker Compose (版本 2.0 或更高)
- 8GB+ 可用内存（推荐）

### ⚡ 性能优化启动（推荐）

为了获得最佳的启动速度，建议使用以下流程：

#### 1. 首次使用 - 预构建依赖缓存
```bash
# 预下载和缓存所有依赖（只需运行一次）
chmod +x bin/prebuild.sh
./bin/prebuild.sh
```

#### 2. 开发模式启动（最快）
```bash
# 使用缓存和优化配置的开发模式
chmod +x bin/start-dev.sh
./bin/start-dev.sh
```

### 🔄 传统启动方式

#### Linux/macOS 用户
```bash
# 给脚本执行权限
chmod +x bin/*.sh

# 启动项目
./bin/start.sh
```

#### Windows 用户
```cmd
# 直接双击运行或在命令行中执行
bin\start.bat
```

## ⚡ 启动模式对比

| 启动模式 | 脚本 | 首次启动时间 | 后续启动时间 | 适用场景 |
|---------|------|-------------|-------------|----------|
| 🚀 开发模式 | `./bin/start-dev.sh` | ~3-5分钟 | ~1-2分钟 | 开发环境，频繁重启 |
| 📦 生产模式 | `./bin/start.sh` | ~5-8分钟 | ~3-5分钟 | 生产环境，稳定运行 |
| 🔧 预构建 | `./bin/prebuild.sh` | ~2-3分钟 | - | 首次使用，缓存依赖 |

### 💡 性能优化特性

#### 开发模式优势：
- ✅ **Maven 依赖缓存** - 避免重复下载依赖包
- ✅ **NPM 依赖缓存** - 前端依赖持久化缓存
- ✅ **Docker 层缓存** - 分离依赖下载和源码编译
- ✅ **开发环境配置** - 启用热重载和调试模式
- ✅ **增量构建** - 只在源码变化时重新编译

#### 构建优化：
- 📦 **多阶段构建** - 减小最终镜像大小
- 🔄 **智能缓存层** - 依赖变化时才重新下载
- 🚀 **并行构建** - 多个服务同时构建

## 📋 服务说明

启动后将包含以下服务：

| 服务名称 | 端口 | 访问地址 | 说明 |
|---------|------|----------|------|
| PostgreSQL 数据库 | 5432 | localhost:5432 | 主数据库 |
| API Premium Gateway | 8081 | http://localhost:8081 | API网关服务 |
| AgentX 后端服务 | 8080 | http://localhost:8080 | 核心后端API |
| AgentX 前端应用 | 3000 | http://localhost:3000 | Web用户界面 |

## 🔐 默认登录账号

项目启动后会自动创建以下默认账号：

### 管理员账号
- **邮箱**: `admin@agentx.ai`
- **密码**: `admin123`
- **用途**: 系统管理员账号

### 测试账号
- **邮箱**: `test@agentx.ai`
- **密码**: `test123`
- **用途**: 普通用户测试账号

> ⚠️ **重要提示**:
> - 建议登录后立即修改默认密码
> - 生产环境请删除测试账号
> - 默认密码仅用于开发和测试环境

## 🔧 数据库管理

### 首次启动
首次启动时会自动：
1. 初始化数据库表结构
2. 创建默认用户账号
3. 插入基础配置数据

### 数据库重置
如果检测到已存在的数据库数据，启动脚本会询问是否重新初始化：

```
检测到已存在的数据库数据
是否重新初始化数据库？这将删除所有现有数据。
注意: 选择 'y' 将清空所有数据库数据！
重新初始化数据库? [y/N] (默认: N):
```

- 选择 `N` (默认): 跳过初始化，使用现有数据
- 选择 `y`: 清空所有数据并重新初始化（包括重新创建默认账号）

### 数据库备份与恢复

#### 备份数据库
```bash
./bin/backup_database.sh
```

#### 恢复数据库
```bash
./bin/restore_database.sh
```

### 数据库连接信息
- **主机**: localhost
- **端口**: 5432
- **数据库**: agentx
- **用户名**: postgres
- **密码**: postgres
- **连接URL**: `jdbc:postgresql://localhost:5432/agentx`

## 🛠️ 管理命令

### 开发模式命令
```bash
# 查看服务状态
docker-compose -f docker-compose.dev.yml ps

# 停止所有服务
docker-compose -f docker-compose.dev.yml down

# 查看日志
docker-compose -f docker-compose.dev.yml logs -f [服务名]

# 重启服务
docker-compose -f docker-compose.dev.yml restart [服务名]
```

### 生产模式命令
```bash
# 查看服务状态
docker-compose ps

# 停止所有服务
./bin/stop.sh
# 或者
docker-compose down

# 查看日志
docker-compose logs -f [服务名]

# 重启服务
docker-compose restart [服务名]
```

### 缓存管理
```bash
# 清理所有缓存
docker volume rm agentx-maven-cache agentx-npm-cache
docker rmi agentx-maven-cache agentx-npm-cache

# 查看缓存大小
docker system df -v
```

## 🐛 故障排除

### 端口冲突
如果遇到端口冲突，请检查以下端口是否被占用：
- 3000 (前端)
- 8080 (后端)
- 8081 (网关)
- 5432 (数据库)

### 构建速度慢
1. **首次启动**: 运行 `./bin/prebuild.sh` 预缓存依赖
2. **使用开发模式**: `./bin/start-dev.sh` 启动更快
3. **检查网络**: 确保网络连接稳定
4. **清理缓存**: 如果缓存损坏，清理后重新构建

### 服务启动失败
1. 检查 Docker 是否正常运行
2. 检查系统资源是否充足
3. 查看服务日志定位具体问题
4. 确保所有必要文件存在

### 数据库连接失败
1. 等待数据库完全启动（通常需要30-60秒）
2. 检查数据库容器状态：`docker ps | grep postgres`
3. 查看数据库日志：`docker-compose logs postgres`

### 登录问题
1. 确认使用的是默认账号：`admin@agentx.ai` / `admin123`
2. 检查后端服务是否正常启动
3. 查看后端日志：`docker-compose logs agentx-backend`

### 清理环境
如果需要完全清理环境：
```bash
# 停止并删除所有容器、网络和数据卷
docker-compose down -v --remove-orphans
docker-compose -f docker-compose.dev.yml down -v --remove-orphans

# 删除所有相关镜像
docker images | grep agentx | awk '{print $3}' | xargs docker rmi

# 清理构建缓存
docker builder prune -f
```

## 🔄 代码更新

### 开发模式更新
```bash
# 停止服务
docker-compose -f docker-compose.dev.yml down

# 重新启动（自动重新构建变更的部分）
./bin/start-dev.sh
```

### 生产模式更新
```bash
# 停止服务
./bin/stop.sh

# 重新启动（会自动重新构建）
./bin/start.sh
```

## 🔧 开发环境

对于开发者：
1. **代码热重载**: 开发模式支持部分热重载
2. **快速重启**: 利用缓存快速重新构建
3. **日志管理**: 日志文件保存在 `logs/` 目录下
4. **数据持久化**: 数据库数据持久化保存，停止容器不会丢失数据
5. **依赖缓存**: Maven/NPM 依赖被持久化缓存

## 📊 性能监控

### 查看资源使用情况
```bash
# 查看容器资源使用
docker stats

# 查看存储使用
docker system df

# 查看缓存卷大小
docker volume ls
```

## 📞 支持

如果遇到问题，请：
1. 查看本文档的故障排除部分
2. 检查服务日志
3. 提交 Issue 到项目仓库

---

🎉 **祝你使用愉快！** 
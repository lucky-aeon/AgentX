# AgentX 一键部署指南

## 🚀 最简单的部署方式

**只需一条命令，AgentX 就能运行！**

```bash
docker run -d \
  --name agentx \
  -p 3000:3000 \
  -p 8080:8080 \
  -v agentx-data:/var/lib/postgresql/data \
  ghcr.io/lucky-aeon/agentx:latest
```

等待 2-3 分钟后访问：
- 🌐 **前端界面**: http://localhost:3000
- 🔌 **后端API**: http://localhost:8080/api

## 👤 默认账户

- **管理员**: admin@agentx.ai / admin123
- **测试用户**: test@agentx.ai / test123

---

## 🎯 这个镜像包含什么？

**AgentX All-in-One 镜像包含完整的系统：**

| 组件 | 端口 | 说明 |
|------|------|------|
| 🌐 Next.js 前端 | 3000 | 用户界面 |
| ⚙️ Spring Boot 后端 | 8080 | API 服务 |
| 💾 PostgreSQL 数据库 | 5432 | 数据存储 |

**特点：**
- ✅ **真正的一键部署** - 不需要 docker-compose
- ✅ **数据持久化** - 数据保存在 Docker 卷中
- ✅ **开箱即用** - 包含所有必要服务
- ✅ **轻量高效** - 基于 Alpine Linux

---

## 🔧 管理命令

### 查看状态
```bash
docker ps | grep agentx
```

### 查看日志
```bash
# 查看启动日志
docker logs agentx

# 实时查看日志
docker logs -f agentx
```

### 重启服务
```bash
docker restart agentx
```

### 停止服务
```bash
docker stop agentx
docker rm agentx
```

### 更新到最新版
```bash
# 停止当前容器
docker stop agentx && docker rm agentx

# 拉取最新镜像
docker pull ghcr.io/lucky-aeon/agentx:latest

# 重新启动（使用原来的启动命令）
docker run -d \
  --name agentx \
  -p 3000:3000 \
  -p 8080:8080 \
  -v agentx-data:/var/lib/postgresql/data \
  ghcr.io/lucky-aeon/agentx:latest
```

---

## ⚙️ 高级配置（可选）

### 使用外部数据库

如果你有现有的 PostgreSQL 数据库：

```bash
docker run -d \
  --name agentx \
  -p 3000:3000 \
  -p 8080:8080 \
  -e DB_HOST=your-db-host \
  -e DB_PORT=5432 \
  -e DB_NAME=agentx \
  -e DB_USERNAME=your-user \
  -e DB_PASSWORD=your-password \
  ghcr.io/lucky-aeon/agentx:latest
```

### 自定义端口

```bash
docker run -d \
  --name agentx \
  -p 8000:3000 \
  -p 9000:8080 \
  -v agentx-data:/var/lib/postgresql/data \
  ghcr.io/lucky-aeon/agentx:latest
```

访问地址变为：
- 前端: http://localhost:8000
- 后端: http://localhost:9000/api

### 连接外部服务

```bash
docker run -d \
  --name agentx \
  -p 3000:3000 \
  -p 8080:8080 \
  -e MCP_GATEWAY_URL=http://your-mcp-gateway:8080 \
  -e HA_ENABLED=true \
  -e HIGH_AVAILABILITY_GATEWAY_URL=http://your-ha-gateway:8081 \
  -v agentx-data:/var/lib/postgresql/data \
  ghcr.io/lucky-aeon/agentx:latest
```

---

## ❓ 故障排除

### 启动慢？
- 首次启动需要初始化数据库，大约 2-3 分钟
- 查看启动进度：`docker logs -f agentx`

### 端口被占用？
```bash
# 检查端口占用
lsof -i :3000
lsof -i :8080

# 使用其他端口
docker run -d --name agentx -p 8000:3000 -p 9000:8080 ...
```

### 数据丢失？
确保使用了数据卷：
```bash
-v agentx-data:/var/lib/postgresql/data
```

### 无法访问？
检查防火墙和容器状态：
```bash
docker ps
docker logs agentx
```

### 容器无法启动？
```bash
# 查看详细错误
docker logs agentx

# 重新拉取镜像
docker pull ghcr.io/lucky-aeon/agentx:latest
```

---

## 🏗️ 架构说明

AgentX 采用现代微服务架构，打包为单一容器：

```
ghcr.io/lucky-aeon/agentx:latest
├── Next.js 前端 (Node.js)
├── Spring Boot 后端 (Java)
└── PostgreSQL 数据库
```

**数据流：**
1. 用户访问前端 (localhost:3000)
2. 前端调用后端 API (localhost:8080/api)
3. 后端连接内置数据库 (localhost:5432)

**优势：**
- 🚀 **一键部署** - 无需复杂配置
- 🔒 **数据安全** - 内置数据库，数据不出容器
- 📦 **便携性强** - 可在任何支持 Docker 的环境运行
- 🎯 **开发友好** - 适合快速原型和测试

---

## 📞 支持

- 📖 **文档**: https://github.com/lucky-aeon/agentx
- 🐛 **问题反馈**: https://github.com/lucky-aeon/agentx/issues
- 💬 **讨论**: https://github.com/lucky-aeon/agentx/discussions

---

**🎉 享受 AgentX 带来的智能体验！**
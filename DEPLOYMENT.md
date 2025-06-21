# AgentX Docker 部署指南

## 🚀 快速部署

### 最简单方式（一条命令）

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

### 快速访问
部署完成后（大约 2-3 分钟）直接访问：
- 🌐 **前端界面**: http://localhost:3000
- 🔌 **后端API**: http://localhost:8080  
- 🚪 **API网关**: http://localhost:8081

### 默认账户
- 👤 **管理员**: admin@agentx.ai / admin123
- 👤 **测试用户**: test@agentx.ai / test123

---

## ⚙️ 自定义配置（可选）

如果需要自定义配置（如数据库密码、邮箱设置、GitHub OAuth 等）：

```bash
# 1. 下载配置文件模板
curl -O https://raw.githubusercontent.com/lucky-aeon/agentx/main/.env.example

# 2. 编辑配置文件
mv .env.example .env
nano .env  # 修改需要的配置项

# 3. 使用自定义配置启动
mkdir -p ./agentx-config && cp .env ./agentx-config/
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

### 主要配置项
- `DB_PASSWORD`: 数据库密码
- `GITHUB_CLIENT_ID/SECRET`: GitHub OAuth 登录
- `MAIL_SMTP_*`: 邮箱服务配置
- `S3_ACCESS_KEY/SECRET`: 对象存储配置

---

## 🔧 管理命令

```bash
# 查看容器状态
docker ps | grep agentx

# 查看日志
docker logs -f agentx

# 重启服务
docker restart agentx

# 停止服务
docker stop agentx && docker rm agentx

# 更新到最新版本
docker pull ghcr.io/lucky-aeon/agentx:latest
docker stop agentx && docker rm agentx
# 然后重新运行上面的启动命令
```

---

## 📦 架构说明

AgentX 采用微服务架构，一个镜像包含所有服务：

| 组件 | 端口 | 说明 |
|------|------|------|
| 前端 | 3000 | Next.js Web 界面 |
| 后端 | 8080 | Spring Boot API 服务 |
| API网关 | 8081 | 高可用网关 |
| MCP网关 | 8005 | MCP 协议网关 |
| 数据库 | 5432/5433 | PostgreSQL 数据存储 |

---

## ❓ 故障排除

**服务启动慢？**
- 首次启动需要拉取依赖镜像，约 2-3 分钟
- 可以通过 `docker logs -f agentx` 查看启动进度

**端口冲突？**
- 修改端口映射：`-p 8000:3000 -p 8888:8080 -p 8889:8081`

**数据持久化？**
- 使用 Docker 卷：`-v agentx-data:/var/lib/docker`
- 数据会自动保存在 Docker 卷中
# AgentX - 智能对话系统平台

[](https://opensource.org/licenses/MIT)

AgentX 是一个基于大模型 (LLM) 和多能力平台 (MCP) 的智能 Agent 构建平台。它致力于简化 Agent 的创建流程，让用户无需复杂的流程节点或拖拽操作，仅通过自然语言和工具集成即可打造个性化的智能 Agent。

## 🔗 相关链接

### 📦 子仓库
- 🛡️ **高可用网关**: [API-Premium-Gateway](https://github.com/lucky-aeon/API-Premium-Gateway) - 模型高可用组件
- 🌐 **MCP网关**: [mcp-gateway](https://github.com/lucky-aeon/mcp-gateway) - MCP服务统一管理
- 🏪 **MCP社区**: [agent-mcp-community](https://github.com/lucky-aeon/agent-mcp-community) - MCP Server 开源社区

### 📚 学习资源
- 🎥 **项目教程**: [B站视频教程](https://www.bilibili.com/video/BV1qaTWzPERJ/?spm_id_from=333.1387.homepage.video_card.click)
- 📖 **详细教学**: [敲鸭社区 - code.xhyovo.cn](https://code.xhyovo.cn/)
- 🎯 **项目演示**: [在线PPT介绍](https://needless-comparison.surge.sh)

## 🚀 快速开始

### 🐳 一键部署（推荐）

适用于想要快速体验完整功能的用户，**无需下载源码**，一个命令启动所有服务：

#### 步骤1：准备配置文件

```bash
# 下载配置文件模板
curl -O https://raw.githubusercontent.com/lucky-aeon/AgentX/master/.env.example
# 复制并编辑配置
cp .env.example .env
# 根据需要修改 .env 文件中的配置
```

#### 步骤2：启动服务

```bash
# 一键启动（包含前端+后端+数据库+消息队列）
# 🎯 智能适配：本地、内网、服务器环境均可使用相同命令
docker run -d \
  --name agentx \
  -p 3000:3000 \
  -p 8088:8088 \
  -p 5432:5432 \
  -p 5672:5672 \
  -p 15672:15672 \
  --env-file .env \
  -v agentx-data:/var/lib/postgresql/data \
  -v agentx-storage:/app/storage \
  -v /var/run/docker.sock:/var/run/docker.sock\
  --add-host=localhost:host-gateway \
  ghcr.nju.edu.cn/lucky-aeon/agentx:latest
```

> 🚀 **智能部署**：无需区分本地或服务器环境，前端自动检测当前访问IP并连接对应的后端服务

#### 访问服务

| 服务 | 地址 | 说明 |
|------|------|------|
| **主应用** | http://localhost:3000 | 前端界面 |
| **后端API** | http://localhost:8088 | API服务 |
| **数据库** | http://localhost:5432 | PostgreSQL（可选） |
| **RabbitMQ** | http://localhost:5672 | 消息队列（可选） |
| **RabbitMQ管理** | http://localhost:15672 | 队列管理界面（可选） |

#### 高可用网关（可选）

如需API高可用功能，可额外部署：

```bash
docker run -d \
  --name agentx-gateway \
  -p 8081:8081 \
  ghcr.io/lucky-aeon/api-premium-gateway:latest
```

**默认登录账号**：
- 管理员：`admin@agentx.ai` / `admin123`
- 测试用户：`test@agentx.ai` / `test123`

#### 🌐 部署场景示例

**本地开发**：
```bash
# 访问: http://localhost:3000
# API自动指向: http://localhost:8088/api ✅
```

**内网服务器**：
```bash
# 访问: http://192.168.1.100:3000
# API自动指向: http://192.168.1.100:8088/api ✅
```

**公网服务器**：
```bash
# 访问: http://your-server-ip:3000
# API自动指向: http://your-server-ip:8088/api ✅

# 访问: http://your-domain.com:3000
# API自动指向: http://your-domain.com:8088/api ✅
```

> 💡 **提示**：生产环境部署前，请在.env文件中修改默认密码和JWT密钥

### 👨‍💻 开发环境部署
适用于需要修改代码或定制功能的开发者：

```bash
# 1. 克隆项目
git clone https://github.com/lucky-aeon/AgentX.git
cd AgentX/deploy

# 2. 启动开发环境（Linux/macOS）
./start.sh

# 2. 启动开发环境（Windows）
start.bat
```

**开发环境特色**：
- 🔥 代码热重载
- 🛠 数据库管理工具
- 🐛 调试端口开放
- 📊 详细开发日志

## ⏳ 功能
 - [x] Agent 管理（创建/发布）
 - [x] LLM 上下文管理（滑动窗口，摘要算法）
 - [x] Agent 策略（MCP）
 - [x] 大模型服务商
 - [x] 用户
 - [x] 工具市场
 - [x] MCP Server Community
 - [x] MCP Gateway 
 - [x] 预先设置工具
 - [x] Agent 定时任务
 - [x] Agent OpenAPI
 - [x] 模型高可用组件
 - [x] RAG
 - [x] 计费
 - [x] Agent 监控
 - [x] 嵌入网站组件
 - [ ] Multi Agent
 - [ ] 知识图谱
 - [x] 长期记忆 
 
## ⚙️ 环境变量配置

AgentX使用`.env`配置文件进行环境变量管理，支持丰富的自定义配置：

### 📁 配置文件说明

| 配置项 | 说明 | 默认值 |
|--------|------|-------|
| **基础服务** |  |  |
| `SERVER_PORT` | 后端API端口 | `8088` |
| `DB_PASSWORD` | 数据库密码 | `agentx_pass` |
| `RABBITMQ_PASSWORD` | 消息队列密码 | `guest` |
| **安全配置** |  |  |
| `JWT_SECRET` | JWT密钥（必须修改） | 需要设置 |
| `AGENTX_ADMIN_PASSWORD` | 管理员密码 | `admin123` |
| **外部服务** |  |  |
| `EXTERNAL_DB_HOST` | 外部数据库地址 | 空（使用内置） |
| `EXTERNAL_RABBITMQ_HOST` | 外部消息队列地址 | 空（使用内置） |

### 🔧 快速配置

```bash
# 1. 获取配置模板
curl -O https://raw.githubusercontent.com/lucky-aeon/AgentX/main/.env.example

# 2. 创建配置文件
cp .env.example .env

# 3. 编辑配置（必改项）
vim .env
```

**必须修改的配置项**：
- `JWT_SECRET`: 设置安全的JWT密钥（至少32字符）
- `AGENTX_ADMIN_PASSWORD`: 修改管理员密码
- `DB_PASSWORD`: 修改数据库密码

### 📝 配置分类

<details>
<summary><strong>🔐 安全配置（重要）</strong></summary>

```env
# 生产环境必须修改
JWT_SECRET=your_secure_jwt_secret_key_at_least_32_characters
AGENTX_ADMIN_PASSWORD=your_secure_admin_password
DB_PASSWORD=your_secure_db_password
RABBITMQ_PASSWORD=your_secure_mq_password
```

</details>

<details>
<summary><strong>🔗 外部服务集成</strong></summary>

```env
# 使用外部数据库
EXTERNAL_DB_HOST=your-postgres-host
DB_HOST=your-postgres-host
DB_USER=your-db-user
DB_PASSWORD=your-db-password

# 使用外部消息队列
EXTERNAL_RABBITMQ_HOST=your-rabbitmq-host
RABBITMQ_HOST=your-rabbitmq-host
RABBITMQ_USERNAME=your-mq-user
RABBITMQ_PASSWORD=your-mq-password
```

</details>

<details>
<summary><strong>☁️ 云服务配置</strong></summary>

```env
# 阿里云OSS
OSS_ENDPOINT=https://oss-cn-beijing.aliyuncs.com
OSS_ACCESS_KEY=your_access_key
OSS_SECRET_KEY=your_secret_key
OSS_BUCKET=your_bucket_name

# AWS S3
S3_SECRET_ID=your_s3_access_key
S3_SECRET_KEY=your_s3_secret_key
S3_REGION=us-east-1
S3_BUCKET_NAME=your_bucket

# AI服务
SILICONFLOW_API_KEY=your_api_key
HIGH_AVAILABILITY_ENABLED=true
HIGH_AVAILABILITY_GATEWAY_URL=http://localhost:8081
```

</details>

<details>
<summary><strong>📧 通知与认证</strong></summary>

```env
# 邮件服务
MAIL_SMTP_HOST=smtp.qq.com
MAIL_SMTP_USERNAME=your_email@qq.com
MAIL_SMTP_PASSWORD=your_email_password

# GitHub OAuth
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret

# 支付服务
ALIPAY_APP_ID=your_alipay_app_id
STRIPE_SECRET_KEY=your_stripe_secret_key
```

</details>

> 📋 **完整配置参考**：查看 [.env.example](/.env.example) 文件了解所有可配置参数



## 📖 部署文档

| 文档 | 说明 |
|------|------|
| [生产部署指南](docs/deployment/PRODUCTION_DEPLOY.md) | 生产环境完整部署 |
| [开发部署指南](deploy/README.md) | 开发者环境配置 |
| [故障排查手册](docs/deployment/TROUBLESHOOTING.md) | 问题诊断和解决 |

## 功能介绍

## Contributors

[![AgentX](https://contrib.rocks/image?repo=lucky-aeon/agentX)](https://contrib.rocks/image?repo=lucky-aeon/agentX)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=lucky-aeon/agentX&type=Date)](https://api.star-history.com/svg?repos=lucky-aeon/agentX&type=Date)


## 联系我们

我们致力于构建一个活跃的开发者社区，欢迎各种形式的交流与合作！

### 📱 私人微信
如有技术问题或商务合作，可添加开发者微信：

<img src="docs/images/wechat.jpg" alt="私人微信" width="200"/>

### 👥 微信交流群
加入我们的技术交流群，与更多开发者一起讨论：

<img src="docs/images/group.jpg" alt="微信交流群" width="200"/>

### 📢 微信公众号
关注我们的公众号，获取最新技术动态和产品更新：

<img src="docs/images/微信公众号.jpg" alt="微信公众号" width="200"/>

---

**如果二维码过期或无法扫描，请通过私人微信联系我。**

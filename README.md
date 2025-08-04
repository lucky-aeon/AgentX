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

### 🐳 生产环境部署（推荐）

#### 基础版部署
适用于想要快速体验核心功能的用户，**无需下载源码**：

```bash
# 一键启动（包含前端+后端+数据库+消息队列）
docker run -d \
  --name agentx-core \
  -p 3000:3000 \
  -p 8088:8088 \
  ghcr.io/lucky-aeon/agentx:latest
```

**访问地址**：http://localhost:3000

#### 完整版部署
如需API网关的高可用功能，可额外部署API网关：

```bash
# 1. 启动核心服务
docker run -d \
  --name agentx-core \
  -p 3000:3000 \
  -p 8088:8088 \
  ghcr.io/lucky-aeon/agentx:latest

# 2. 启动API网关（可选）
docker run -d \
  --name agentx-gateway \
  -p 8081:8081 \
  ghcr.io/lucky-aeon/api-premium-gateway:latest
```

**访问地址**：
- 主应用：http://localhost:3000  
- API网关：http://localhost:8081

#### 功能对比

| 功能 | 基础版 | 完整版 |
|------|--------|--------|
| AI助手对话 | ✅ | ✅ |
| 知识库管理 | ✅ | ✅ |  
| 工具市场 | ✅ | ✅ |
| API高可用 | ❌ | ✅ |
| 负载均衡 | ❌ | ✅ |
| API监控 | ❌ | ✅ |

**默认账号**：
- 管理员：`admin@agentx.ai` / `admin123`

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
 - [ ] Multi Agent
 - [ ] Agent 监控
 - [ ] 知识图谱
 - [ ] 长期记忆 
 
## ⚙️ 环境变量配置

AgentX支持通过环境变量进行灵活配置。创建 `.env` 文件：

### 🗄️ 数据库配置
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=agentx
DB_USER=postgres
DB_PASSWORD=your_secure_password
```

### 🐰 消息队列配置
```env
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=your_rabbitmq_password
```

### 👤 系统用户配置
```env
AGENTX_ADMIN_EMAIL=admin@agentx.ai
AGENTX_ADMIN_PASSWORD=admin123
AGENTX_ADMIN_NICKNAME=AgentX管理员
AGENTX_TEST_ENABLED=true
AGENTX_TEST_EMAIL=test@agentx.ai
AGENTX_TEST_PASSWORD=test123
```

### 📧 邮件服务配置（可选）
```env
MAIL_SMTP_HOST=smtp.qq.com
MAIL_SMTP_PORT=587
MAIL_SMTP_USERNAME=your_email@qq.com
MAIL_SMTP_PASSWORD=your_email_password
```

### 🔐 OAuth配置（可选）
```env
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_REDIRECT_URI=https://your-domain/oauth/github/callback
```
### ☁️ 对象存储配置（可选）
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
S3_ENDPOINT=https://s3.amazonaws.com
S3_BUCKET_NAME=your_bucket
```

### 🤖 AI服务配置（可选）
```env
SILICONFLOW_API_KEY=your_api_key
SILICONFLOW_API_URL_RERANK=https://api.siliconflow.cn/v1/rerank
MCP_GATEWAY_URL=http://localhost:8005
```

### 💳 支付配置（可选）
```env
# 支付宝配置
ALIPAY_APP_ID=your_alipay_app_id
ALIPAY_PRIVATE_KEY=your_alipay_private_key
ALIPAY_PUBLIC_KEY=your_alipay_public_key

# Stripe配置
STRIPE_SECRET_KEY=your_stripe_secret_key
STRIPE_PUBLISHABLE_KEY=your_stripe_publishable_key
```

<details>
<summary>查看完整环境变量列表</summary>

包含高可用网关、向量数据库等更多配置选项，请查看完整的 `application.yml` 文件了解所有可配置参数。

</details>



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
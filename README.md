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
 - [ ] Agent OpenAPI
 - [x] 模型高可用组件
 - [ ] RAG
 - [ ] 计费
 - [ ] Multi Agent
 - [ ] Agent 监控

## 🚀 如何安装启动

### 🛠️ 环境准备

  * **Docker & Docker Compose**: 用于容器化部署（推荐）
  * **Node.js & npm**: 推荐使用 LTS 版本（本地开发）
  * **Java Development Kit (JDK)**: JDK 17 或更高版本（本地开发）

### 🐳 一键启动（推荐）

#### 🔥 热更新开发模式

**最佳开发体验**：代码修改实时生效，无需重启容器！

```bash
# 克隆仓库
git clone https://github.com/lucky-aeon/AgentX.git
cd AgentX

# 一键启动热更新模式
./bin/start-dev.sh --hot
```

#### 🚀 标准开发模式

```bash
# 标准开发模式（重启生效）
./bin/start-dev.sh
```

#### 🏭 生产模式

```bash
# 生产环境启动
./bin/start.sh
```

### 📋 服务访问地址

启动成功后，您可以通过以下地址访问服务：

- **前端应用**: http://localhost:3000
- **后端API**: http://localhost:8080
- **API网关**: http://localhost:8081
- **数据库连接**: localhost:5432

### 🔐 默认登录账号

系统会自动创建以下默认账号：

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 管理员 | admin@agentx.ai | admin123 |
| 测试用户 | test@agentx.ai | test123 |

⚠️ **安全提示**：首次登录后请立即修改默认密码，生产环境请删除测试账号。

### 🛠️ 开发管理命令

```bash
# 查看服务状态
docker compose -f docker-compose.hotreload.yml ps

# 停止所有服务
docker compose -f docker-compose.hotreload.yml down

# 查看服务日志
docker compose -f docker-compose.hotreload.yml logs -f [服务名]

# 重启特定服务
docker compose -f docker-compose.hotreload.yml restart [服务名]
```

### 💻 本地开发启动（传统方式）

如果您更喜欢传统的本地开发方式：

#### 1\. 启动数据库

```bash
cd script
chmod +x setup_with_compose.sh
./setup_with_compose.sh
```

#### 2\. 启动后端服务

```bash
cd AgentX
./mvnw spring-boot:run
```

#### 3\. 启动前端服务

```bash
cd agentx-frontend-plus
npm install --legacy-peer-deps
npm run dev
```

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


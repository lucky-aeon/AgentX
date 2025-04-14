# AgentX - 智能对话系统平台

AgentX 是一个 agent 平台，通过大模型 + MCP 方式来打造属于你的 agent，这里没有复杂的流程节点！没有复杂的拖拉拽！你只需要添加工具！你只需要使用自然语言！你就能打造属于你的 agent！

## 项目启动
### 前端启动

npm install --legacy-peer-deps

## 项目结构

项目采用DDD（领域驱动设计）架构，主要包含以下几个层次：

- **接口层(Interfaces)**: 负责与外部系统交互，包括API接口、前端界面等
- **应用层(Application)**: 负责业务流程编排，调用领域服务完成业务逻辑
- **领域层(Domain)**: 包含核心业务逻辑和领域模型
- **基础设施层(Infrastructure)**: 提供技术支持，如数据持久化、外部服务等

## 技术栈

- **后端**: Java 17+, Spring Boot 3.x
- **数据库**: PostgreSQL 14.x + pgvector
- **容器化**: Docker & Docker Compose

## 开发环境搭建

### 前置条件

- JDK 17+
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL 14+ (可选，也可使用Docker启动)

## 功能模块

- **基础对话功能**: 流式对话、会话管理、上下文管理
- **服务商管理**: 多模型服务商接入、服务商配置管理
- **知识库功能**: 文档管理、向量存储、RAG检索增强
- **MCP**: 对接 MCP Server 完成工具的调用
- **用户系统与计费**: 用户认证、计费系统、使用统计
- **市场功能**: 插件市场、工具市场、知识库市场
- **API与集成**: 对外API、SDK、外部系统集成
- **定时任务**: 自动化 agent

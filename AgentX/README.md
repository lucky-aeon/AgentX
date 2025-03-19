# AgentX - 智能对话系统平台

AgentX是一个基于大语言模型的智能对话系统平台，支持多种LLM服务商，提供灵活易用的API接口。

## 主要功能

- 支持多种LLM服务商集成（SiliconFlow等）
- 统一的对话API接口
- 灵活的会话管理
- 可扩展的插件系统

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- PostgreSQL 12+

### 配置文件

应用配置文件位于 `src/main/resources/application.yml`，主要配置项：

```yaml
# LLM服务配置
llm:
  provider:
    default: ${LLM_DEFAULT_PROVIDER:siliconflow}  # 默认使用的服务商
    providers:
      siliconflow:  # SiliconFlow服务商配置
        name: SiliconFlow
        api-url: ${SILICONFLOW_API_URL:https://api.siliconflow.cn/v1/chat/completions}
        api-key: ${SILICONFLOW_API_KEY:你的API密钥}
        model: ${SILICONFLOW_MODEL:Qwen/Qwen2.5-VL-72B-Instruct}
        timeout: ${SILICONFLOW_TIMEOUT:30000}
```

可以通过环境变量或命令行参数覆盖默认配置：

```bash
# 设置环境变量
export SILICONFLOW_API_KEY=你的API密钥
export SILICONFLOW_MODEL=llama3

# 或者通过命令行参数启动
java -jar AgentX.jar --SILICONFLOW_API_KEY=你的API密钥 --SILICONFLOW_MODEL=llama3
```

### API接口

#### 对话接口

```
POST /api/conversation/chat
```

请求参数：

```json
{
  "message": "你好，请介绍一下自己",
  "sessionId": "session-123",  // 可选，会话ID
  "provider": "siliconflow",  // 可选，服务商名称
  "model": "llama3"  // 可选，模型名称
}
```

响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "content": "我是一个AI助手...",
    "sessionId": "session-123",
    "provider": "SiliconFlow",
    "model": "llama3",
    "timestamp": 1623234567890
  },
  "timestamp": 1623234567890
}
```

## 服务商支持

目前支持以下服务商：

- SiliconFlow

## 项目结构

项目采用DDD（领域驱动设计）架构，主要包含以下几个层次：

- **接口层(Interfaces)**: 负责与外部系统交互，包括API接口、前端界面等
- **应用层(Application)**: 负责业务流程编排，调用领域服务完成业务逻辑
- **领域层(Domain)**: 包含核心业务逻辑和领域模型
- **基础设施层(Infrastructure)**: 提供技术支持，如数据持久化、外部服务等

## 技术栈

- **后端**: Java 1.8, Spring Boot 2.7.x
- **数据库**: PostgreSQL 14.x + pgvector
- **容器化**: Docker & Docker Compose

## 开发环境搭建

### 前置条件

- JDK 1.8+
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL 14+ (可选，也可使用Docker启动)

### 环境准备

1. 克隆项目

```bash
git clone https://github.com/xhyqaq/AgentX
cd AgentX
```

2. 使用Maven构建项目

```bash
mvn clean install
```

3. 使用Docker Compose启动环境

```bash
docker-compose up -d
```

### 开发指南

项目使用Maven管理依赖，使用Spring Boot作为开发框架。开发新功能时请遵循DDD架构设计原则，将不同职责的代码放在相应的层次中。

## 功能模块

- **基础对话功能**: 流式对话、会话管理、上下文管理
- **服务商管理**: 多模型服务商接入、服务商配置管理
- **知识库功能**: 文档管理、向量存储、RAG检索增强
- **函数调用与工具**: 函数调用框架、系统工具、自定义工具
- **用户系统与计费**: 用户认证、计费系统、使用统计
- **市场功能**: 插件市场、服务商市场、工具市场
- **API与集成**: 对外API、SDK、外部系统集成

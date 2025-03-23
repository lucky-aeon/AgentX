# AgentX 技术文档

## 1. 技术架构

### 1.1 总体架构

AgentX采用分层架构设计，主要包含以下几个层次：

- **表示层**：负责与用户交互的前端界面和API接口
- **应用层**：实现业务逻辑和功能编排
- **领域层**：封装核心业务模型和领域规则
- **基础设施层**：提供数据持久化、第三方服务集成等基础功能

系统整体架构如下：

```
┌─────────────────────────────────────────┐
│              表示层 (Presentation)        │
│  ┌──────────────┐      ┌──────────────┐  │
│  │    Web UI    │      │  REST/WS API │  │
│  └──────────────┘      └──────────────┘  │
└─────────────────────────────────────────┘
                 │
┌─────────────────────────────────────────┐
│              应用层 (Application)         │
│  ┌──────────┐  ┌──────────┐ ┌─────────┐  │
│  │会话服务   │  │服务商服务 │ │工具服务 │  │
│  └──────────┘  └──────────┘ └─────────┘  │
│  ┌──────────┐  ┌──────────┐ ┌─────────┐  │
│  │知识库服务 │  │用户服务   │ │市场服务 │  │
│  └──────────┘  └──────────┘ └─────────┘  │
└─────────────────────────────────────────┘
                 │
┌─────────────────────────────────────────┐
│              领域层 (Domain)             │
│  ┌──────────┐  ┌──────────┐ ┌─────────┐  │
│  │对话领域   │  │知识库领域 │ │工具领域 │  │
│  └──────────┘  └──────────┘ └─────────┘  │
│  ┌──────────┐  ┌──────────┐ ┌─────────┐  │
│  │服务商领域 │  │用户领域   │ │市场领域 │  │
│  └──────────┘  └──────────┘ └─────────┘  │
└─────────────────────────────────────────┘
                 │
┌─────────────────────────────────────────┐
│            基础设施层 (Infrastructure)    │
│  ┌──────────┐  ┌──────────┐              │
│  │数据持久化 │  │外部服务   │              │
│  └──────────┘  └──────────┘              │
│                ┌──────────┐              │
│                │日志服务   │              │
│                └──────────┘              │
└─────────────────────────────────────────┘
```

### 1.2 模块划分

根据业务功能，系统划分为以下核心模块：

1. **对话模块**：处理用户对话、上下文管理和会话管理
2. **服务商模块**：管理多种LLM服务商接入和调用
3. **知识库模块**：文档处理、向量化和检索
4. **工具模块**：函数调用框架和工具管理
5. **用户模块**：用户认证、权限和数据管理
6. **计费模块**：用量统计、计费和支付
7. **市场模块**：插件、服务商和工具市场
8. **API模块**：对外API接口和SDK

## 2. 技术选型

### 2.1 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 1.8 | 编程语言 |
| Spring Boot | 2.7.x | 应用框架 |
| PostgreSQL | 14.x | 关系型数据库 |
| pgvector | 0.5.x | PostgreSQL向量扩展 |
| MyBatis Plus | 3.5.x | ORM框架 |
| JWT | - | 认证机制 |
| Docker | - | 容器化部署 |

### 2.2 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | 前端框架 |
| TypeScript | 4.x | 编程语言 |
| PrimeVue | 3.x | UI组件库 |
| Pinia | 2.x | 状态管理 |
| Axios | - | HTTP客户端 |
| Socket.IO | 4.x | WebSocket客户端 |

### 2.3 部署环境

- Docker + Docker Compose
- JDK 1.8
- PostgreSQL 14+
- Nginx (前端部署和反向代理)

## 3. 数据库设计

### 3.1 数据库概述

系统使用PostgreSQL作为主要数据库，并通过pgvector扩展支持向量检索功能。数据库主要包含以下几类数据：

1. **用户数据**：存储用户信息、认证数据、组织信息和API密钥
2. **Agent数据**：保存Agent信息、配置和与会话的关联
3. **对话数据**：保存会话信息、消息内容和上下文
4. **服务商数据**：管理服务商配置和模型信息
5. **知识库数据**：存储文档、文档块和向量嵌入
6. **工具数据**：保存工具定义和执行记录
7. **计费数据**：记录订阅计划、用户余额、使用量和交易记录
8. **市场数据**：存储市场项目、评价和安装记录

详细的表结构设计将在系统实现阶段逐步完善。

### 3.2 数据库索引策略

- 对查询频繁的字段创建索引
- 对向量字段使用专用向量索引（IVFFlat、HNSW等）
- 对JSON字段中常用查询路径创建GIN索引
- 合理使用联合索引优化复杂查询

### 3.3 Agent数据库表设计

#### 3.3.1 agent表

存储Agent的基本信息和配置。

```sql
CREATE TABLE agent (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT 'Agent名称',
    avatar VARCHAR(255) COMMENT 'Agent头像URL',
    description TEXT COMMENT 'Agent描述',
    system_prompt TEXT COMMENT 'Agent系统提示词',
    welcome_message TEXT COMMENT '欢迎消息',
    model_config JSONB COMMENT '模型配置，包含模型类型、温度等参数',
    tools JSONB COMMENT 'Agent可使用的工具列表',
    knowledge_base_ids JSONB COMMENT '关联的知识库ID列表',
    is_private BOOLEAN DEFAULT TRUE COMMENT '是否为私有Agent',
    user_id BIGINT COMMENT '创建者用户ID',
    status SMALLINT DEFAULT 1 COMMENT 'Agent状态：0-禁用，1-启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);

-- 创建索引
CREATE INDEX idx_agent_user_id ON agent(user_id);
CREATE INDEX idx_agent_status ON agent(status);
CREATE INDEX idx_agent_name ON agent(name);
```

#### 3.3.2 会话表(conversation)修改

将会话表修改为与Agent关联。

```sql
CREATE TABLE conversation (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL COMMENT '会话标题',
    agent_id BIGINT NOT NULL COMMENT '关联的Agent ID',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    last_message_time TIMESTAMP COMMENT '最后消息时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (agent_id) REFERENCES agent(id)
);

-- 创建索引
CREATE INDEX idx_conversation_agent_id ON conversation(agent_id);
CREATE INDEX idx_conversation_user_id ON conversation(user_id);
```

以上是基本的Agent管理相关数据库设计，它定义了Agent实体以及与会话的关联关系。系统将基于这些表结构实现Agent的创建、配置、管理和与会话的关联功能。

## 4. 核心功能实现

### 4.1 流式对话

使用WebSocket或SSE（Server-Sent Events）技术实现流式响应，通过异步处理聊天请求并实时返回生成结果，使用户获得接近实时的对话体验。

### 4.2 服务商抽象

设计统一的服务商接口，便于接入不同的模型服务。接口将定义基本功能如文本生成、流式响应、向量嵌入等能力，各服务商（如OpenAI、Claude等）只需实现此接口即可被系统集成。

### 4.3 向量检索

利用PostgreSQL的pgvector扩展实现向量检索，将文档内容向量化并存储，支持高效的相似度搜索，为RAG（检索增强生成）提供基础。

### 4.4 函数调用框架

实现工具调用的核心框架，定义标准工具接口和注册机制，支持工具的动态注册和调用，使大模型能够使用外部工具扩展其能力。

### 4.5 计费系统

实现基础的计费功能，包括用量记录、余额管理、套餐订阅等，支持按使用量（如请求次数、Token量）计费，并提供余额查询和充值功能。

## 5. 安全设计

### 5.1 认证与授权

- 基于JWT的认证机制
- 基于角色的访问控制

### 5.2 数据安全

- 敏感数据加密存储
- API密钥安全管理
- 数据传输加密(HTTPS)

### 5.3 工具执行安全

- 安全的工具执行环境
- 资源使用限制
- 代码注入防护

## 6. 部署架构

### 6.1 Docker部署

使用Docker Compose组织多容器应用，简化部署和环境配置过程。主要包含应用服务、数据库服务、前端服务等容器。

## 7. 系统扩展性

### 7.1 插件系统

实现基础的插件系统，支持功能扩展和自定义开发，包括插件的加载、管理和配置等基本功能。 
# 简历编写（1.0 版本）
**项目名称：AgentX 平台**  
**项目地址：**

[https://github.com/lucky-aeon/AgentX](https://github.com/lucky-aeon/AgentX)

[https://github.com/lucky-aeon/API-Premium-Gateway](https://github.com/lucky-aeon/API-Premium-Gateway)  
[https://github.com/lucky-aeon/mcp-gateway](https://github.com/lucky-aeon/mcp-gateway)  
[https://github.com/lucky-aeon/agent-mcp-community](https://github.com/lucky-aeon/agent-mcp-community)

**项目描述：**  
AgentX 平台是一款致力于零学习成本打造个性化 AI Agent 的全栈应用。作为该项目的**独立原创开发者**，我全面负责了从产品构思、需求分析、架构设计、多仓库开发、到最终的运维部署全过程。项目核心在于通过自然语言驱动 Agent 的构建、发布与使用，并创新性地集成了自研高可用组件、先进的 RAG 混合搜索框架、MCP 协议网关及精细化 Token 上下文管理，旨在为用户提供高性能、稳定、智能的 Agent 体验。

**核心技术：**  
`JDK17` | `SpringBoot3` | `DDD` | `PostgreSQL` | `langchain4j` | `S3` | `自定义MCP网关` | `自研高可用项目` 

**个人职责：**  
**独立负责 AgentX 平台从 0 到 1 的全生命周期，涵盖产品与需求规划、多仓库（后端、前端、网关、社区）核心功能开发、深入修改并定制 **`**langchain4j**`** 框架源码以实现工具预设参数功能、自研高可用组件及 RAG 框架构建，直至运维部署与优化。**

**项目内容：**

+ **Agent 核心管理：** 负责 Agent 的生命周期管理（创建、配置、版本、发布、工作区），提供多 Agent 协同与 OpenAPI 接口。
+ **Token 策略：** 内置 `langchain4j` 上下文处理机制，并实现了基于 Token 的滑动窗口及摘要算法，优化长对话管理。
+ **模型服务商集成：** 抽象统一的模型接入层，支持多 LLM 服务商的配置与管理。
+ **工具/插件管理：** 允许用户上传自定义工具，通过状态机流程管理工具审核，并与 MCP 网关及 MCP Server Community 交互，实现工具的动态挂载与调用。
+ **MCP 网关 (Multi-Agent Communication Protocol Gateway)：** 作为核心通信枢纽，负责 MCP 协议转换、Agent 请求负载均衡、工作区隔离，并管理与监控 MCP Server 实例。
+ **MCP Server Community：** 负责执行 Agent 任务的分布式运行环境，提供 Agent 的实际执行能力。
+ **模型高可用服务：** 自研高可用组件，对集成 LLM 提供负载均衡、熔断降级、限流等策略，确保模型服务的稳定性和可靠性。
+ **RAG (检索增强生成) 框架：** 融合向量与分词检索，引入 Reranker 模型重排及标题加权，优化高亮聚焦长文本与日期衰减排序，结合动态词库与双检索模式（Match/Match_phrase），显著提升搜索准确性与用户体验。
+ **计费服务：** 基于模型 Token 使用量和预设计费规则，实现灵活、精准的计费与扣费逻辑。



# 开发规范&项目环境搭建
开发规范参考：[https://github.com/lucky-aeon/AgentX/blob/master/docs/develop_document.md](https://github.com/lucky-aeon/AgentX/blob/master/docs/develop_document.md)

**模型服务商的注册**

1.可以通过我的邀请码注册可以送免费额度 [https://cloud.siliconflow.cn/i/pe3O5ZKE](https://cloud.siliconflow.cn/i/pe3O5ZKE)

2.通过模型广场里面的 API 文档查看[https://cloud.siliconflow.cn/models](https://cloud.siliconflow.cn/models)

3.API 密钥在：[https://cloud.siliconflow.cn/account/ak](https://cloud.siliconflow.cn/account/ak)

# 流式输出 & 助理管理与发布
## 流式输出
大模型的完整回复输出等待时间较长，不过大模型提供了流式输出，因此为了让用户体验更加友好，项目中支持流式输出响应。并且使用 SSE

**SSE & WebSocket**

| 特性 | **SSE** | **WebSocket** |
| --- | --- | --- |
| **协议** | 基于HTTP协议（单向通信） | 基于TCP协议（双向通信） |
| **通信方向** | 服务器 -> 客户端 | 双向：服务器和客户端都可以发送消息 |
| **连接保持** | 客户端与服务器之间的HTTP连接保持长时间 | 使用WebSocket协议进行持久化的双向TCP连接 |
| **使用场景** | 适用于单向数据流，如实时推送通知、股票价格等 | 适用于需要双向交互的场景，如聊天室、游戏等 |
| **支持的浏览器** | 大部分现代浏览器支持（如Chrome, Firefox等） | 大部分现代浏览器支持（如Chrome, Firefox等） |
| **消息格式** | 文本/JSON格式（可扩展的文本消息） | 二进制（如BLOB、ArrayBuffer）和文本（如JSON）格式 |
| **连接数限制** | 适合单个连接，限制较少 | 适合大量并发连接，但服务器需要处理更多的负载 |
| **连接的复杂性** | 相对简单，基于标准的HTTP协议 | 需要使用WebSocket API进行初始化和连接管理 |
| **传输延迟** | 比WebSocket稍高，但延迟较低 | 通常低延迟，尤其在需要快速双向通讯时表现较好 |
| **消息大小** | 有部分浏览器限制每个消息大小（如 1MB） | 没有大小限制，适合传输较大的二进制数据 |
| **心跳机制** | 自动处理，无需额外配置 | 需要自行处理心跳机制，以保持连接活动状态 |
| **可靠性** | HTTP协议本身支持自动重连 | WebSocket需要自定义重连机制 |
| **服务器实现** | 比较简单，不需要额外的协议栈或复杂的控制 | 需要WebSocket服务器的支持，增加实现复杂度 |


## 助理管理与发布
**需求：**

创建的 Agent 需要让 TA 人也能使用，因此会有 Agent 的管理与发布。



**表结构**



分成三张表：agents，agent_versions，agent_workspace

agents：基本信息

agent_versions：agent 发布后的信息，是快照，不可修改的

agent_workspace：用户在添加 agent 的时候用来保存的，涉及到用户在和 agent 进行对话的时候，设置需要的模型，大模型的参数，token 上下文策略



**agents**

| 字段名 | 数据类型 | 约束/默认值 | 说明/注释 |
| --- | --- | --- | --- |
| id | character varying(36) | PRIMARY KEY, NOT NULL | Agent唯一ID |
| name | character varying(255) | NOT NULL | Agent名称 |
| avatar | character varying(255) | - | Agent头像URL |
| description | text | - | Agent描述 |
| system_prompt | text | - | Agent系统提示词 |
| welcome_message | text | - | 欢迎消息 |
| tool_ids | jsonb | - | 可使用的工具ID列表（JSON格式） |
| published_version | character varying(36) | - | 当前发布的版本ID |
| enabled | boolean | DEFAULT TRUE | Agent状态：TRUE-启用，FALSE-禁用 |
| user_id | character varying(36) | NOT NULL | 创建者用户ID |
| tool_preset_params | jsonb | - | 工具预设参数（JSON格式） |
| multi_modal | boolean | DEFAULT FALSE | 是否支持多模态（默认不支持） |
| created_at | timestamp without time zone | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | timestamp without time zone | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| deleted_at | timestamp without time zone | - | 逻辑删除时间（非NULL表示已删除） |
| knowledge_base_ids | jsonb | - | 关联的知识库ID列表（JSON数组格式，用于RAG功能） |


**agent_versions**

| 字段名 | 数据类型 | 约束/默认值 | 说明/注释 |
| --- | --- | --- | --- |
| id | character varying(36) | PRIMARY KEY, NOT NULL | 版本唯一ID |
| agent_id | character varying(36) | NOT NULL | 关联的Agent ID |
| name | character varying(255) | NOT NULL | Agent名称（当前版本的名称） |
| avatar | character varying(255) | - | Agent头像URL（当前版本的头像） |
| description | text | - | Agent描述（当前版本的描述） |
| version_number | character varying(20) | NOT NULL | 版本号（如1.0.0） |
| system_prompt | text | - | Agent系统提示词（当前版本） |
| welcome_message | text | - | 欢迎消息（当前版本） |
| tool_ids | jsonb | - | 可使用的工具ID列表（JSON数组格式，当前版本） |
| knowledge_base_ids | jsonb | - | 关联的知识库ID列表（JSON数组格式，当前版本） |
| change_log | text | - | 版本更新日志 |
| publish_status | integer | DEFAULT 1 | 发布状态：1-审核中, 2-已发布, 3-拒绝, 4-已下架 |
| reject_reason | text | - | 审核拒绝原因（publish_status=3时有效） |
| review_time | timestamp without time zone | - | 审核时间 |
| published_at | timestamp without time zone | - | 发布时间（publish_status=2时有效） |
| user_id | character varying(36) | NOT NULL | 创建者用户ID |
| tool_preset_params | jsonb | - | 工具预设参数（JSON格式，当前版本） |
| multi_modal | boolean | DEFAULT FALSE | 当前版本是否支持多模态（默认不支持） |
| created_at | timestamp without time zone | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | timestamp without time zone | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| deleted_at | timestamp without time zone | - | 逻辑删除时间（非NULL表示已删除） |




**agent_workspace**

| 字段名 | 数据类型 | 约束/默认值 | 说明/注释 |
| --- | --- | --- | --- |
| id | character varying(36) | PRIMARY KEY, NOT NULL | 主键ID |
| agent_id | character varying(36) | NOT NULL | 关联的Agent ID |
| user_id | character varying(36) | NOT NULL | 用户ID（添加到工作区的用户） |
| llm_model_config | jsonb | - | 模型配置（JSON格式，用户自定义的模型参数） |
| created_at | timestamp without time zone | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间（添加到工作区的时间） |
| updated_at | timestamp without time zone | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新时间（配置修改时间） |
| deleted_at | timestamp without time zone | - | 逻辑删除时间（非NULL表示已从工作区移除） |




# Token 上下文策略


Token 计算网站：[https://tiktoken.aigc2d.com/](https://tiktoken.aigc2d.com/)



和大模型进行交互的最小单元是 Token，虽然我们和大模型对话是通过自然语言，但是实际大模型会转为 Token进行交互，而大模型的 Token 可以理解为大模型的记忆，也就是说如果大模型 Token 越多，记忆越长，直至永久记忆。



但是现在的模型没有永久的记忆， Token 都是有上限的，看到的一些产品说永久记忆，其实是对于 Token 进行了压缩，或者通过 RAG 的方式，外部文件的方式来达到 “永久记忆”。



token 上下文策略指的是在和大模型进行对话的过程中 token 快满了，该如何处理？ 如果不处理的话，则代表大模型的记忆满了，你就无法和大模型再进行对话了，因此我们需要做处理。



在项目中提供了俩种方式处理：

1.Token 滑动窗口

2.摘要算法



**Token 滑动窗口实现原理**

滑动窗口可以理解为外面一个大窗口包含 N 个小窗口，假设大窗口上限是 100，有 11 个小窗口，每个小窗口占用 10，则需要把最后一个小窗口给抛弃，这就是滑动窗口。



对于 Token 滑动窗口就可以理解为，每个窗口的大小是 Token，总窗口是大模型支持最大的 Token 数，如果用户当前消息的 Token 加上之前的窗口大于了总窗口，则需要抛弃掉消息，而抛弃的消息的数量会根据 Token 进行计算抛弃

****

**摘要算法实现原理**

摘要算法可以理解为压缩消息算法，在消息达到阈值的时候可以对消息进行压缩，来达到减少 Token，但是这样也会导致可能会丢弃一些信息，毕竟是压缩算法，会压缩一些信息来减少 Token。



**可以参考 claude code 的 /compact**

****

摘要算法有利也有弊，利是可以不丢掉所有消息，不像 Token 滑动窗口，弊是可以总结消息



**表结构 **

message：历史消息表，存储所有的消息

context：上下文表，和大模型用来对话时候的可用消息（active_messages）



messages

| 字段名 | 数据类型 | 约束/默认值 | 说明/注释 |
| --- | --- | --- | --- |
| id | character varying(36) | PRIMARY KEY, NOT NULL | 消息唯一ID |
| session_id | character varying(36) | NOT NULL | 所属会话ID（关联到具体对话会话） |
| role | character varying(20) | NOT NULL | 消息角色（user-用户、assistant-助手、system-系统） |
| content | text | NOT NULL | 消息内容 |
| message_type | character varying(20) | NOT NULL, DEFAULT 'TEXT' | 消息类型（默认TEXT，可扩展为其他类型如IMAGE、FILE等） |
| token_count | integer | DEFAULT 0 | Token数量（消息内容对应的令牌数，用于计费或长度控制） |
| provider | character varying(50) | - | 服务提供商（如OpenAI、Anthropic等） |
| model | character varying(50) | - | 使用的模型（如gpt-3.5-turbo、claude-2等） |
| metadata | jsonb | - | 消息元数据（JSON格式，可存储额外信息如消息状态、扩展属性等） |
| file_urls | jsonb | - | 关联文件URL列表（JSON数组格式，如图片、文档等附件链接） |
| created_at | timestamp without time zone | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | timestamp without time zone | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| deleted_at | timestamp without time zone | - | 逻辑删除时间（非NULL表示已删除） |




context

| 字段名 | 数据类型 | 约束/默认值 | 说明/注释 |
| --- | --- | --- | --- |
| id | character varying(36) | PRIMARY KEY, NOT NULL | 上下文唯一ID |
| session_id | character varying(36) | NOT NULL | 所属会话ID（与messages表的session_id关联） |
| active_messages | jsonb | - | 活跃消息ID列表（JSON数组格式，记录当前上下文窗口中有效的消息ID，用于控制对话上下文范围） |
| summary | text | - | 历史消息摘要（对早期对话内容的压缩总结，减少上下文长度） |
| created_at | timestamp without time zone | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | timestamp without time zone | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新时间（上下文窗口或摘要更新时触发） |
| deleted_at | timestamp without time zone | - | 逻辑删除时间（非NULL表示已删除） |


# 大模型服务商
**需求**

个人在进行对接的时候直接使用 apiKey，baseUrl，model 就可以进行对接了，但是我们是一个平台，用户可以自行创建服务商以及模型，平台也会提供官方的服务商模型给到用户进行使用



**用户自己创建的服务商，所涉及到的 apiKey 一定是要加密的**以下是计费系统数据库表结构的表格形式展示：

### **1. rules（计费规则表）**
| **字段名** | **数据类型** | **约束/默认值** | **说明/注释** |
| --- | --- | --- | --- |
| **id** | **VARCHAR(64)** | **NOT NULL, PRIMARY KEY** | **规则ID** |
| **name** | **VARCHAR(255)** | **NOT NULL** | **规则名称** |
| **handler_key** | **VARCHAR(100)** | **NOT NULL** | **处理器标识，对应策略枚举** |
| **description** | **TEXT** |  | **规则描述** |
| **deleted_at** | **TIMESTAMP** |  | **软删除时间** |
| **created_at** | **TIMESTAMP** | **DEFAULT CURRENT_TIMESTAMP** | **创建时间** |
| **updated_at** | **TIMESTAMP** | **DEFAULT CURRENT_TIMESTAMP** | **更新时间** |


### **2. products（计费商品表）**
| **字段名** | **数据类型** | **约束/默认值** | **说明/注释** |
| --- | --- | --- | --- |
| **id** | **VARCHAR(64)** | **NOT NULL, PRIMARY KEY** | **商品ID** |
| **name** | **VARCHAR(255)** | **NOT NULL** | **商品名称** |
| **type** | **VARCHAR(50)** | **NOT NULL** | **计费类型：MODEL_USAGE(模型调用)、AGENT_CREATION(Agent创建)、AGENT_USAGE(Agent使用)、API_CALL(API调用)、STORAGE_USAGE(存储使用)** |
| **service_id** | **VARCHAR(100)** | **NOT NULL** | **业务服务标识** |
| **rule_id** | **VARCHAR(64)** | **NOT NULL** | **关联的规则ID** |
| **pricing_config** | **JSONB** |  | **价格配置（JSONB格式）** |
| **status** | **INTEGER** | **DEFAULT 1** | **状态：1-激活，0-禁用** |
| **deleted_at** | **TIMESTAMP** |  | **软删除时间** |
| **created_at** | **TIMESTAMP** | **DEFAULT CURRENT_TIMESTAMP** | **创建时间** |
| **updated_at** | **TIMESTAMP** | **DEFAULT CURRENT_TIMESTAMP** | **更新时间** |


### **3. accounts（用户账户表）**
| **字段名** | **数据类型** | **约束/默认值** | **说明/注释** |
| --- | --- | --- | --- |
| **id** | **VARCHAR(64)** | **NOT NULL, PRIMARY KEY** | **账户ID** |
| **user_id** | **VARCHAR(64)** | **NOT NULL** | **用户ID** |
| **balance** | **DECIMAL(20,8)** | **DEFAULT 0.00000000** | **账户余额** |
| **credit** | **DECIMAL(20,8)** | **DEFAULT 0.00000000** | **信用额度** |
| **total_consumed** | **DECIMAL(20,8)** | **DEFAULT 0.00000000** | **总消费金额** |
| **last_transaction_at** | **TIMESTAMP** |  | **最后交易时间** |
| **deleted_at** | **TIMESTAMP** |  | **软删除时间** |
| **created_at** | **TIMESTAMP** | **DEFAULT CURRENT_TIMESTAMP** | **创建时间** |
| **updated_at** | **TIMESTAMP** | **DEFAULT CURRENT_TIMESTAMP** | **更新时间** |


### **4. usage_records（使用记录表）**
| **字段名** | **数据类型** | **约束/默认值** | **说明/注释** |
| --- | --- | --- | --- |
| **id** | **VARCHAR(64)** | **NOT NULL, PRIMARY KEY** | **记录ID** |
| **user_id** | **VARCHAR(64)** | **NOT NULL** | **用户ID** |
| **product_id** | **VARCHAR(64)** | **NOT NULL** | **商品ID** |
| **quantity_data** | **JSONB** |  | **使用量数据（JSONB格式）** |
| **cost** | **DECIMAL(20,8)** | **NOT NULL** | **本次消费金额** |
| **request_id** | **VARCHAR(255)** | **NOT NULL** | **请求ID（幂等性保证）** |
| **billed_at** | **TIMESTAMP** | **DEFAULT CURRENT_TIMESTAMP** | **计费时间** |
| **deleted_at** | **TIMESTAMP** |  | **软删除时间** |
| **created_at** | **TIMESTAMP** | **DEFAULT CURRENT_TIMESTAMP** | **创建时间** |
| **updated_at** | **TIMESTAMP** | **DEFAULT CURRENT_TIMESTAMP** | **更新时间** |


**以上表格清晰展示了四个表的字段结构、数据类型、约束条件及字段说明，便于理解整个计费系统的数据库设计。**

****

服务商指的是模型服务商，代表着大家使用模型对应的服务商，一个服务商下面会有多个模型

****



**表结构**

providers：服务商信息

models：模型



一个服务商下面会有多个模型，模型也会区分对话模型还是嵌入模型，后续在 RAG 的时候方便筛选，模型中的 model_id 可以用来做高可用





providers

| **字段名** | **数据类型** | **约束/默认值** | **说明/注释** |
| --- | --- | --- | --- |
| **id** | **character varying(36)** | **PRIMARY KEY, NOT NULL** | **服务提供商ID（唯一标识）** |
| **user_id** | **character varying(36)** | **-** | **用户ID（非必填，可能用于关联自定义服务提供商的创建者）** |
| **protocol** | **character varying(50)** | **NOT NULL** | **协议类型（如OpenAI API、Anthropic API等，定义服务交互的协议标准）** |
| **name** | **character varying(100)** | **NOT NULL** | **服务提供商名称（如“OpenAI”“阿里云通义千问”等）** |
| **description** | **text** | **-** | **服务提供商描述（如服务特点、支持的模型类型等）** |
| **config** | **text** | **-** | **服务提供商配置（加密后的内容，可能包含API密钥、访问地址等敏感信息）** |
| **is_official** | **boolean** | **DEFAULT FALSE** | **是否官方服务提供商（TRUE-官方内置，FALSE-用户自定义）** |
| **status** | **boolean** | **DEFAULT TRUE** | **服务提供商状态（TRUE-启用，FALSE-禁用）** |
| **created_at** | **timestamp without time zone** | **NOT NULL, DEFAULT CURRENT_TIMESTAMP** | **创建时间** |
| **updated_at** | **timestamp without time zone** | **NOT NULL, DEFAULT CURRENT_TIMESTAMP** | **更新时间（配置或状态修改时触发）** |
| **deleted_at** | **timestamp without time zone** | **-** | **逻辑删除时间（非NULL表示已删除）** |




models

| **字段名** | **数据类型** | **约束/默认值** | **说明/注释** |
| --- | --- | --- | --- |
| **id** | **character varying(36)** | **PRIMARY KEY, NOT NULL** | **模型ID（系统内唯一标识）** |
| **user_id** | **character varying(36)** | **-** | **用户ID（非必填，关联自定义模型的创建者）** |
| **provider_id** | **character varying(36)** | **NOT NULL** | **服务提供商ID（关联providers表的id，标识模型所属的服务提供商）** |
| **model_id** | **character varying(100)** | **NOT NULL** | **模型ID标识（服务提供商侧的模型唯一标识，如“gpt-4o”“claude-3-opus”）** |
| **name** | **character varying(100)** | **NOT NULL** | **模型名称（如“GPT-4o”“通义千问X”等，便于用户理解的名称）** |
| **model_endpoint** | **character varying(255)** | **NOT NULL** | **模型调用端点（API请求的URL地址，如**`**https://api.openai.com/v1/chat/completions**`**）** |
| **description** | **text** | **-** | **模型描述（如模型能力、适用场景、参数规模等）** |
| **is_official** | **boolean** | **DEFAULT FALSE** | **是否官方模型（TRUE-官方内置，FALSE-用户自定义）** |
| **type** | **character varying(20)** | **NOT NULL** | **模型类型（如“LLM”“IMAGE”“EMBEDDING”等，区分模型功能）** |
| **status** | **boolean** | **DEFAULT TRUE** | **模型状态（TRUE-启用，FALSE-禁用）** |
| **created_at** | **timestamp without time zone** | **NOT NULL, DEFAULT CURRENT_TIMESTAMP** | **创建时间** |
| **updated_at** | **timestamp without time zone** | **NOT NULL, DEFAULT CURRENT_TIMESTAMP** | **更新时间（配置或状态修改时触发）** |
| **deleted_at** | **timestamp without time zone** | **-** | **逻辑删除时间（非NULL表示已删除）** |




# Agent 策略
Agent：LLM（大模型） + 工具



大模型只能和人类进行对话，没有办法帮助人类处理任务，因此就有了工具的概念，工具指的是和大模型对话的过程中，想让大模型处理任务的过程中，大模型知道有哪些工具，可以进行调用，这里的调用指的不是大模型调用，而是大模型知道处理这个任务需要调用这个工具，而工具是需要人从代码层面进行调用。可以这样理解，你把一个调用天气工具的接口告诉大模型（提示词），你现在跟大模型说：我想知道今天天气，那么大模型就知道要处理这个任务，需要调用该工具，然后把工具信息返回给人类，人类通过代码层面判断即可调用对应的工具。



在起初的时候工具的调用会通过 Function Calling 的方式（没学过的可以看 AI 专栏的视频），在现在出了 MCP 的方式，其实本质都是工具，只是实现的方式不一样而已，不用管 Function Calling 还是 MCP 还是未来的等等，大家记住一个本质：把工具的信息塞到提示词里面，大模型知道了，就该知道怎么调。也就是说本质还是提示词



~~Agent 策略有很多种方式，文字太多，建议大家从视频中学习了（码字，码不动）~~



# 插件 - 工具市场
**需求**

agent 的组成：llm + 工具，因此我们就要实现工具，而工具市场指的是大家都可以上传工具，TA 人也可以使用



工具的实现方式采用 MCP Server，项目只需要通过 MCP Client 即可进行连接使用。因此用户上传自己的 MCP Service 即可在项目中使用到工具



**MCP是什么 todo 介绍**

****

**上传工具介绍**

上传工具要涉及到安全问题，获取工具列表（工具的元信息，不可能让用户填，而是我们获取），源仓库保存，因此上传工具需要进行审核，审核涉及到多个状态，这里采用状态机进行实现，审核流程如下：

1.审核 Github Url （后续是否可以保存到 MCP 社区）

2.审核工具是否能部署到 MCP 网关（工具本身是否健康）

3.获取工具列表（填充到工具元信息）

4.人工审核（审核基本信息，审核仓库信息）

5.发布到 MCP 社区仓库

通过

****

**工具发布与管理**

需要 TA 人使用工具，需要将工具进行发布，而工具的发布不需要经过审核，因为在上传工具的时候已经过审核，而发布的逻辑跟 Agent 一样，是快照信息，不可更改，用户在使用的时候也是使用的版本快照数据，而不是源数据



**优化点：**

在删除工具的时候，按照常理会将发布的工具以及其他用户安装的工具也一并删除，但是如果删除了其他用户已安装的工具的话，就会导致其他用户的 Agent 如果引用了该工具，则会无法可用。因此用户在删除工具的时候，删除源工具和发布后的工具，但是已安装的工具不允许删除，而是要告知已安装该工具的用户一个提示：“该工具源已删除，后续不再维护”。





**设计：**

在上传工具中的涉及到多个状态的扭转，我们可以使用状态机实现

状态机：A->B->C->D

![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1747755486684-36919bc0-cab7-437b-a3c9-1884d5c65199.png)



# Agent 预先设置工具参数


**需求**

Agent 中携带工具，而有些工具的参数可以提前设置，这样，TA 人在使用的时候就可以省去自己配置的步骤，例如：



![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1747923030511-98c0b9ee-ea7a-440f-baeb-5dd07a12fc52.png)



你开发了一个前端网站部署助手，涉及到俩个工具：file-system,surge（部署前端用的），而 surge 的使用需要账号密码，如果你将这个 Agent 发布出去给别人使用，那么别人在使用的时候还需要使用 surge 的账号密码，这对于使用者来说体验很差（你可能会想，我的密钥凭什么给别人用？但是确实有这种场景）



那么应该如何处理？如果将密钥设置到系统提示词中，是可以解决的，但是使用者可以通过提示词注入的方式拿到密钥



我们可以通过在调用工具的时候直接将参数传入来实现这个需求，这样的话，只需要告诉大模型，xxx 工具已经设置好参数，可以直接调用，但是 langchan4j 未能实现，因此我们自己实现即可





在 agent 中需要添加额外字段：预先设置工具参数字段，结构如下：

```java
[
    {
"<tool_name>": [
            {
                "<工具名称>": {
                    "参数":"value"
                }
            }
        ]
    }
]
```

**修改的源码**

```java
McpClient client = new DefaultMcpClient.Builder().transport(transport)
                   .build();
            client.presetParameters(Collections.singletonList(
                    new PresetParameter("surge_login","{\"email\": \"xxx@qq.com\", \"password\": \"xxx\"}")));
            mcpClients.add(client);
```

DefaultMcpClient

```java
private ToolExecutionRequest tryPresetParameter(ToolExecutionRequest executionRequest) {
        String name = executionRequest.name();
        if (presetParameterMap.containsKey(name)) {
            String presetParameter = presetParameterMap.get(name);
            ToolExecutionRequest.Builder builder = ToolExecutionRequest.builder().id(executionRequest.id()).name(name).arguments(presetParameter);
            return builder.build();
        }
        return executionRequest;
    }
```

效果能实现，但是问题是：这种的提前设置，大模型是不知道的，很可能会 “找用户索要信息”，那用户就不得不在系统提示词里面编写对应的策略。 那这样也会增加用户的心智负担

**解决方案**

只能通过修改框架 + 系统提示词拼接的方式即可



# langchan4j 框架二开&部署
**需求背景**

因为需要实现 “预先设置工具” 需求，但是 langchan4j 不支持，因此需要自己改源码并且发布使用





对框架不满意，需要修改源码，并且使用：

1. 本地修改部署
2. 修改推向 maven
3. jitpack 方式

**区别**

| **特性** | **Maven Central Repository** | **JitPack** |
| :--- | :--- | :--- |
| **发布方式** | **复杂，需要 Sonatype JIRA 申请、GPG 签名、Maven 配置等。** | **简单，只需将代码推送到 Git 仓库并创建 Release/Tag。** |
| **构建方式** | **开发者本地构建并上传预编译好的构件。** | **JitPack 服务器按需从 Git 仓库拉取代码并构建。** |
| **构件来源** | **预编译好的 JARs、Javadoc、Sources JARs 等。** | **从 Git 仓库源码构建的 JARs。** |
| **依赖引用** | **默认支持，无需额外配置仓库。** | **需要在 **`**pom.xml**`**/**`**build.gradle**`** 中额外添加 JitPack 仓库。** |
| **信任与安全** | **高度信任，构件经过 GPG 签名验证。** | **相对较低，直接从 Git 仓库构建，无强制签名。** |
| **稳定性** | **构件一旦发布不可更改，保证构建的稳定性和可重复性。** | **理论上 Tag 或 Commit ID 对应的构建可能发生变化（尽管有缓存）。** |
| **适用场景** | **正式发布的开源库、企业内部库、对稳定性和信任度有高要求的项目。** | **个人项目、快速原型、小型开源库、不想经历复杂发布流程的项目。** |


**何时选择哪个？**

+ **如果你想让你的 Java 库成为业界标准，被更多开发者发现和使用，并且愿意投入时间去学习和遵循发布流程，那么 ****Maven Central Repository**** 是最佳选择。**
+ **如果你只是想快速分享一个个人项目、一个实验性库，或者不想处理复杂的发布流程，那么 ****JitPack**** 是一个非常方便快捷的选择。**

**总的来说，Maven Central 更“专业”和“正式”，而 JitPack 更“便捷”和“轻量”。**



# Agent  定时任务
**需求**

有些任务是需要重复执行的，例如：定时爬取数据发表文章等，因此需要有一个定时任务执行 agent 发送内容

**技术方案**

**1.Scheduled （基于注解的形式）**

**2.定时任务线程池（线程池）**

**3.延迟队列（jdk 原生自带的延迟队列）**



因为定时任务需要动态可管理，因此采用延迟队列

# 用户设置
**需求**

因为 agent 平台中有些地方涉及到使用默认的模型来完成需求，例如：

+ 通过提示词方式创建 agent
+ 创建 agent 的系统提示词通过模型生成
+ 创建 agent 的对话预览



这些地方都不是像目前的设计从服务商中选择模型，而是使用默认配置的模型来完成的。因此现在需要完成这个需求



# 预览对话 & 多模态对话 & s3 协议
**需求**

**预览对话**

在没有创建 Agent 的时候需要对 Agent 进行测试，因此需要有预览对话，预览对话是不会存在会话以及消息保存的，是一个临时的会话



**多模态**

目前的平台只支持文本对话，不支持多模态对话，并且多模态的方式还需要大模型支持，因此在创建/编辑 Agent 的时候发布者需要根据已有的模型进行开关，开启后则在对话框中出现上传组件



**s3协议：亚马逊**

s3是一种协议，相对于直接使用对应服务商的对象存储来说更有规范性，拓展性更强。因此在项目中首选 s3，以对应的服务商配置（ak，sk 等）来请求 s3



# 高可用项目
**仓库**

[**https://github.com/lucky-aeon/API-Premium-Gateway**](https://github.com/lucky-aeon/API-Premium-Gateway)

**需求**

使用的模型一旦不可用（欠费，限流等），以及用户自己的号池想负载均衡。因此我们需要做一个模型高可用来达到：负载均衡（平替），降级：



我们可以将思维发散一下，可以将模型看作是 API，那么我们这个项目就可以是 API 高可用项目，而并非单单对于我们的 AgentX



**为了项目的复用性，可以让多个项目都可以注册使用，通过 apiKey 的方式鉴权**

****

**亲和性**

在模型的负载均衡中，模型本身带了缓存，这个缓存指的是，使用同一个模型进行多轮对话会有缓存，如果中途切换了模型会导致缓存失效，因此我们需要设计亲和性，如果在一个会话中，则一直使用一个模型



也可以看作这样一个场景：假设你在广东地区，你请求了云服务商，云服务商是负载均衡的，分别在：广东，北京，上海。那么我们期望的是一直请求广东，因为这样的话延迟是最低的，这就是亲和性



**流程图**

项目启动



![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1749879193836-1d0e1ccb-9d63-4d1d-9012-cfe70407b5b9.png)

****



选择 api 实例

![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1749879415320-33e0656a-87fd-494a-879c-ee591a8c3c3d.png)

![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1749879228140-acf3ec37-adbf-4314-aac9-594dca6e94a8.png)



api 实例更新

![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1749879351931-39756ad9-2716-424e-99a7-b03bc9dced1f.png)



架构

![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1749879176266-551b52e0-bf1f-4821-8f73-5d807e80286f.png)



# Open-API
**需求**

设计好的 Agent，用户想要在项目中进行使用，因此我们需要开放一系列的 Open-API 给到用户使用



开放的接口还涉及到接口文档，接口文档的暴露维护也至关重要



直接采用 apifox 的方式暴露维护，因为我们的开放接口可以直接通过 IDE 的插件同步到 apifox 中，以及 apifox 会暴露一个域名以及页面美观



# 容器管理 & MCP 网关
MCP 网关仓库：[https://github.com/lucky-aeon/mcp-gateway](https://github.com/lucky-aeon/mcp-gateway)

**需求**

MCP 网关的作用是统一 MCP 的协议，MCP 的管理

容器是 MCP 网关部署的方式，容器的实现可以有很多种，这里采用 Docker，而容器化是为了 MCP 网关部署以及调用工具的过程中达到安全化





**容器管理**

**在项目中容器的具体实现是 Docker**

在项目中可以理解把容器 = MCP 网关，因为启动容器就等于启动 MCP 网关

****

在项目中，容器有三种类型：

1.审核容器：用来审核用户上传的工具

2.用户容器：每个用户拥有的容器：非全局的工具使用的时候就会产生

3.全局容器：全局工具的时候会用到



在使用工具的时候，工具分为：无状态/有状态，对应的是：全局/非全局。如果一个工具能够被共享使用，则是无状态，反之是有状态



如果用户使用的工具都是无状态的，则可以直接使用全局容器，而不需要启动用户容器，因为用户容器做的是工具隔离的效果，如果用户使用的工具只要有一个是有状态的，都要启动用户容器



容器的管理无非就是 CRUD，这里就不再赘述简单代码，不过容器的 CRUD 还需要针对容器自身做一些管理，例如：进入终端，查看容器负载信息等。需要保证容器表和 Docker 的数据一致性，因为会有这种情况： “容器表记录的状态是运行，但是 docker 是暂停/删除”。因此遇到这种情况需要将 docker 运行。



**定时任务处理容器**

容器不能一直运行，因此我们会有定时策略，每个一段时间将未活跃的容器的暂停掉，再每隔一段时间，如果未活跃的时间更长了，则删除容器，但是只是删除容器，表记录不能删除，表记录会根据状态来区分容器状态。 这样也可以处理另外一个问题：因为每个用户都会被分配到容器，而容器的部署是需要端口的，而已知的端口是 65535，也就是说端口会被分配完，因此我们可以通过短时间内的定时任务快速销毁容器，不过也别担心，docker 的网络模式分为 host 和 bridge，前者分配和宿主机关联的端口，后者是容器内的端口



**MCP网关**

MCP 网关承载了 MCP 的协议转换：STDIO 转 SSE，工作区，会话隔离，负载均衡的职责



**协议转换**

使用的时候统一协议，将用户上传的 STDIO 转为 SSE，在使用的过程中方便



**工作区**

工具需要隔离，容器是用户隔离，工作区是容器内的工具隔离。俩者的区别：

用户容器崩了，不会影响其他用户，这就是用户容器的隔离

工作区隔离指的是，假设 5 个 Agent 都使用了一个有状态的工具，Agent 之间没做隔离，甚至 Agent 内的会话也需要做隔离，因此工作区对于 AgentX 代表的就是会话隔离



**会话隔离**

此会话和工作区的会话非彼会话，此会话指的是在 MCP Client 和 MCP  Server 之间连接的时候的会话隔离，因为 MCP Server 是广播的概念，如果是多个 MCP Client 和 MCP Server 交互的话，会被广播到所有的 MCP Client，所以需要有会话隔离。



不过会话隔离也可以由写 MCP Server 的作者来实现



# RAG - 检索增强生成
**<font style="color:rgb(0, 0, 0) !important;">核心原理</font>**

<font style="color:rgba(0, 0, 0, 0.85) !important;">RAG 的工作流程可分为三个关键步骤，形成 “检索 - 增强 - 生成” 的闭环：</font>

1. **<font style="color:rgb(0, 0, 0) !important;">检索（Retrieval）</font>**
    - <font style="color:rgba(0, 0, 0, 0.85) !important;">当用户输入问题时，系统先从</font>**<font style="color:rgb(0, 0, 0) !important;">外部知识库</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">（如文档、数据库、网页等）中检索与问题相关的信息片段。</font>
    - <font style="color:rgba(0, 0, 0, 0.85) !important;">检索方式通常依赖向量数据库：将用户问题和知识库内容转化为向量（通过嵌入模型，如 Sentence-BERT），再通过相似度算法（如余弦相似度）匹配最相关的内容。</font>
2. **<font style="color:rgb(0, 0, 0) !important;">增强（Augmentation）</font>**
    - <font style="color:rgba(0, 0, 0, 0.85) !important;">将检索到的相关信息与用户问题结合，形成新的输入 prompt，传递给生成模型。</font>
    - <font style="color:rgba(0, 0, 0, 0.85) !important;">例如：“基于以下信息回答问题：[检索到的内容]。问题：[用户的问题]”。</font>
3. **<font style="color:rgb(0, 0, 0) !important;">生成（Generation）</font>**
    - <font style="color:rgba(0, 0, 0, 0.85) !important;">生成模型（如 GPT、LLaMA 等）基于增强后的 prompt，结合自身知识生成回答，确保内容既符合上下文，又忠于检索到的外部信息。</font>

**<font style="color:rgb(0, 0, 0) !important;">优势</font>**

1. **<font style="color:rgb(0, 0, 0) !important;">提升准确性</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：减少大模型 “幻觉”（虚构信息），回答可追溯到具体来源（如引用文档段落）。</font>
2. **<font style="color:rgb(0, 0, 0) !important;">增强时效性</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：无需重新训练模型，通过更新知识库即可让模型获取最新信息（如行业动态、政策变化）。</font>
3. **<font style="color:rgb(0, 0, 0) !important;">支持领域专业化</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：可针对特定领域（如医疗、法律、企业内部文档）定制知识库，生成专业回答。</font>
4. **<font style="color:rgb(0, 0, 0) !important;">降低成本</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：相比微调大模型，RAG 通过更新知识库实现知识迭代，成本更低、更灵活。</font>



**应用场景**

1. **问答系统：**企业客服，法律咨询等问答的
2. **<font style="color:rgb(0, 0, 0) !important;">知识管理：</font>**<font style="color:rgb(0, 0, 0) !important;">对企业内部的数据进行存储，通过问答的方式进行搜索，比传统的方式搜索的更精准</font>
3. **<font style="color:rgb(0, 0, 0) !important;">相似度匹配：</font>**<font style="color:rgb(0, 0, 0) !important;">既然 RAG 本质是相似度匹配，那么就可以来实现兴趣推送中的标签推送算法</font>

**<font style="color:rgb(0, 0, 0) !important;"></font>**

<font style="color:rgb(0, 0, 0) !important;"></font>

**<font style="color:rgb(0, 0, 0) !important;">技术实现</font>**

**<font style="color:rgb(0, 0, 0) !important;">向量化：</font>**

<font style="color:rgb(0, 0, 0) !important;">将文档进行切割成片段，每个片段进行向量化，向量化后将向量化的内容和文件进行关联</font>

<font style="color:rgb(0, 0, 0) !important;"></font>

**<font style="color:rgb(0, 0, 0) !important;">搜索：</font>**

<font style="color:rgb(0, 0, 0) !important;">1.将用户的消息向量化  
</font><font style="color:rgb(0, 0, 0) !important;">2.向量化匹配：匹配出来多条数据  
</font><font style="color:rgb(0, 0, 0) !important;">3.rerank</font>

<font style="color:rgb(0, 0, 0) !important;">4.交给大模型，大模型从中筛选</font>

<font style="color:rgb(0, 0, 0) !important;"></font>

**详细技术介绍**

**文档分割**

| **<font style="color:rgb(0, 0, 0) !important;">切割方式</font>** | **<font style="color:rgb(0, 0, 0) !important;">核心依据</font>** | **<font style="color:rgb(0, 0, 0) !important;">优势</font>** | **<font style="color:rgb(0, 0, 0) !important;">劣势</font>** | **<font style="color:rgb(0, 0, 0) !important;">适用场景</font>** |
| :--- | :--- | :--- | :--- | :--- |
| <font style="color:rgba(0, 0, 0, 0.85) !important;">固定长度切割</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">字符 /token 数</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">简单高效</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">破坏语义完整性</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">无格式短文档、日志文件</font> |
| <font style="color:rgba(0, 0, 0, 0.85) !important;">语义结构切割</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">句子 / 段落语义</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">语义连贯</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">依赖 NLP 模型，成本高</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">长文本、主题明确的文档</font> |
| <font style="color:rgba(0, 0, 0, 0.85) !important;">格式结构切割</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">标题 / 章节等格式</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">符合阅读习惯</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">依赖文档格式</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">结构化文档（PDF、Word、网页）</font> |
| <font style="color:rgba(0, 0, 0, 0.85) !important;">内容类型切割</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">文本 / 表格 / 图片等</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">适配多模态内容</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">技术复杂</font> | <font style="color:rgba(0, 0, 0, 0.85) !important;">含表格 / 图片的多类型文档</font> |


**向量化**

指的是将数据（文本/图片/视频）通过嵌入模型转为向量值的过程，值是一组数字，例如：

```plain
[-0.06098073,0.029679552,-0.028717889,0.0034223879,...]
```

向量化输出是有维度的，例如：<font style="color:rgba(0, 0, 0, 0.85);">768 维、1536 维，等</font>

<font style="color:rgba(0, 0, 0, 0.85);">不同的维度场景不一样，并不是维度越高效果越好</font>

<font style="color:rgba(0, 0, 0, 0.85);"></font>

<font style="color:rgba(0, 0, 0, 0.85);">低纬度计算快，语义精度低</font>

<font style="color:rgba(0, 0, 0, 0.85);">高纬度计算慢，语义精度高</font>

<font style="color:rgba(0, 0, 0, 0.85);"></font>



语义化匹配

**向量化匹配**

在向量化和向量化进行匹配是通过算法进行计算的，例如：

1.余弦相似度

2.欧氏距离

3.曼哈顿距离



~~**算法之间的差异大家自行了解，我不是这方面的专家无法讲授了...**~~

~~****~~

**rerank**

rerank 指的是重排序，这是因为在搜索出来的数据是乱序的，需要将最接近答案的进行一个排序，这个排序可以让大模型在选择答案的时候更加精准，指的是：注意力集中。



大模型在识别数据是有注意力集中的，可能会忽略某些数据，而为了让大模型更能精准的识别数据，因此需要提高该数据在大模型面前的“曝光度”，让排序的意义就是为了将更准确的消息排到前面，因为大模型更多的会在前半部分的消息有更多的注意力



**大模型二次筛选**

虽然通过计算得到了相关的数据，但是如果不交给大模型二次审查，而直接将答案给到用户的话，可能数据是有问题的，大模型做的是审核的角色



**文档内容扩展**

因为文档会被拆分成多个片段，在后续搜索匹配到的片段，假设有：A,B,C，其实这三个片段都有关联，但是只搜索到了 B，因此文档拓展将 B 旁边的文档也纳入。本质是不丢上下文



# RAG 发布管理 - RAG 和 Agent 对接
RAG 的管理和 Agent ，Tool 管理是同理的，都是需要审核+快照的方式。



RAG 和其他方式不同的流程：

Tool：上传工具后直接触发审核，审核必须通过才能使用，因为上传的工具是个黑盒，避免用户上传病毒工具

RAG：上传的知识库就能直接使用，因为不会影响到平台，如果需要让 TA 人使用则需要发布，并且因为知识库涉及到内容频繁更新，那么在 Agent 中使用的时候也需要兼容，因此将 0.0.1 版本制定为引用类型的版本，指向源知识库



**上传知识库**

用户在上传知识库后和工具同理，默认创建一个 0.0.1 版本为其安装，不同的是对于知识库来说 0.0.1 版本是引用类型



**使用知识库**

在使用知识库的时候需要判断如果 0.0.1 版本则使用源知识库进行使用



**RAG对接Agent**

多次强调 Agent = LLM + 插件，因此 RAG 的方式可以通过内置封装成 Tool 来完成和 Agent 的对接



# 计费


项目中涉及到官方模型，因此需要对官方模型进行计费



**设计**

如果实现这个需求，可以直接对于在添加官方模型的时候添加计价即可（输入/输出 token 定价）



**但是我们要高度抽象一下这个需求，不要为了实现而实现。可以把模型抽象为商品，把计价抽象为规则。**

规则并不单单是计费规则，而是规则，不只是为了商品服务，在未来，涉及到其他的规则也可以使用该规则

再设想：如果把模型抽象为商品，但是只有官方模型是商品，非官方模型不是商品，这样设计就会导致字段冗余，能否再高度抽象？将商品和模型拆开，模型还是关注与自身，模型想要是一个商品，则让商品关联模型，就代表模型是一个商品，这样设计就可以让任何模块都能成为一个商品，只要和商品关联



**架构设计图**

![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1754123559103-f3c118f5-ac99-4f5c-bea9-322aa1fbc95f.png)

**流程图**



![](https://cdn.nlark.com/yuque/0/2025/png/29091062/1754123580936-c9c1c769-5c47-4915-baa8-a8683b22af05.png)



**表设计**

规则表保证自身干净，不和任务表关联，其他表关联规则表，规则表中的 handler_key 代表对应的规则处理器，在代码中对应的是规则处理类，用来接收其他业务的参数进行计算

****

rule：规则表

| 字段名 | 数据类型 | 约束/默认值 | 说明/注释 |
| --- | --- | --- | --- |
| id | VARCHAR(64) | NOT NULL, PRIMARY KEY | 规则ID |
| name | VARCHAR(255) | NOT NULL | 规则名称 |
| handler_key | VARCHAR(100) | NOT NULL | 处理器标识，对应策略枚举 |
| description | TEXT |  | 规则描述 |
| deleted_at | TIMESTAMP |  | 软删除时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |


 product：商品表

| 字段名 | 数据类型 | 约束/默认值 | 说明/注释 |
| --- | --- | --- | --- |
| id | VARCHAR(64) | NOT NULL, PRIMARY KEY | 商品ID |
| name | VARCHAR(255) | NOT NULL | 商品名称 |
| type | VARCHAR(50) | NOT NULL | 计费类型：MODEL_USAGE(模型调用)、AGENT_CREATION(Agent创建)、AGENT_USAGE(Agent使用)、API_CALL(API调用)、STORAGE_USAGE(存储使用) |
| service_id | VARCHAR(100) | NOT NULL | 业务服务标识 |
| rule_id | VARCHAR(64) | NOT NULL | 关联的规则ID |
| pricing_config | JSONB |  | 价格配置（JSONB格式） |
| status | INTEGER | DEFAULT 1 | 状态：1-激活，0-禁用 |
| deleted_at | TIMESTAMP |  | 软删除时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |




account：账户表

| 字段名 | 数据类型 | 约束/默认值 | 说明/注释 |
| --- | --- | --- | --- |
| id | VARCHAR(64) | NOT NULL, PRIMARY KEY | 账户ID |
| user_id | VARCHAR(64) | NOT NULL | 用户ID |
| balance | DECIMAL(20,8) | DEFAULT 0.00000000 | 账户余额 |
| credit | DECIMAL(20,8) | DEFAULT 0.00000000 | 信用额度 |
| total_consumed | DECIMAL(20,8) | DEFAULT 0.00000000 | 总消费金额 |
| last_transaction_at | TIMESTAMP |  | 最后交易时间 |
| deleted_at | TIMESTAMP |  | 软删除时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |


usage_records：消耗记录表

| 字段名 | 数据类型 | 约束/默认值 | 说明/注释 |
| --- | --- | --- | --- |
| id | VARCHAR(64) | NOT NULL, PRIMARY KEY | 记录ID |
| user_id | VARCHAR(64) | NOT NULL | 用户ID |
| product_id | VARCHAR(64) | NOT NULL | 商品ID |
| quantity_data | JSONB |  | 使用量数据（JSONB格式） |
| cost | DECIMAL(20,8) | NOT NULL | 本次消费金额 |
| request_id | VARCHAR(255) | NOT NULL | 请求ID（幂等性保证） |
| billed_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 计费时间 |
| deleted_at | TIMESTAMP |  | 软删除时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |






# 支付


内网穿透(可选)：cpolar



前端 qr_code：npm install qrcode @types/qrcode --legacy-peer-deps



支付宝支付沙箱：

[https://open.alipay.com/develop/sandbox/app](https://open.alipay.com/develop/sandbox/app)

支付宝 sdk：

[https://github.com/alipay/alipay-easysdk/tree/master/java](https://github.com/alipay/alipay-easysdk/tree/master/java)

[https://opendocs.alipay.com/open/02np95?pathHash=1726489f](https://opendocs.alipay.com/open/02np95?pathHash=1726489f)



stripe：

[https://dashboard.stripe.com/test/dashboard](https://dashboard.stripe.com/test/dashboard)

stripe的 ak/sk/银行卡查看：

[https://dashboard.stripe.com/test/apikeys](https://dashboard.stripe.com/test/apikeys)

[https://docs.stripe.com/testing#cards](https://docs.stripe.com/testing#cards)





项目中支持两个支付平台以及支付类型：

+ 支付宝（国内）：二维码，网站，移动端，APP...
+ Stripe（国外）：网站



在国内的充值平台是支付宝/微信，支付类型是二维码，因为二维码足够方便，移动端就需要唤起 APP 进行支付，这里不考虑移动端。因此平台只会暴露二维码的支付方式，Stripe 部署到海外就可以使用了，但是不代表项目中没有实现，项目中的支付宝的二维码，网站都已支持，Stripe 也支持，只是前端显示支付宝的二维码，可以通过接口 传入不同的参数使用不同的支付平台和支付类型



**Stripe**

小知识：stripe 是海外比较常用的一个支付平台，本身是支持微信，支付宝国内平台的，但是需要向 stripe 进行登记才可支持，否则也无法支持



**支付的流程**

平台目前只涉及到充值的场景，因此只需要前端传入金额，创建订单即可，不是根据商品来进行购买的



1. 根据前端传入金额创建二维码订单
2. 前端开始轮询订单状态
3. 用户支付后，前端轮询查询到订单已支付/支付平台 Callback 已支付，完成订单支付成功的逻辑
4. 修改订单状态为成功：只有支付中的订单状态才可被修改
5. 发起订单支付成功的事件
6. 后续不同业务监听该事件进行处理：此处是余额充值，因此是余额充值事件监听进行余额充值



**限流**

目前的订单需要做限流控制，否则用户可以刷爆我们的订单表，这里暂且使用 guava 

****



****

****

****

****

****

**今年最具有竞争力的 AI 项目： AgentX，业务 + AI 组合，让你不在只学习到 AI 的内容，适合 Agent 岗位，后端岗位，和网上的 AI 项目：Agent+MCP+RAG 不同，我们是一个平台，做了很多优化处理的地方，而这个项目完全开源，让我们来看一下这个项目已有的功能：xxx。接下来给大家演示一下项目：xxxx，**

**.......**

**这个项目的课程学习可以通过我的社区来完整学习或者在 b 站有免费的版本**

****








# AgentX 后端源码导读（DDD 视角）

这份文档不是普通的项目说明，而是一份面向源码阅读的导读手册。目标是让你顺着 `AgentX` 后端代码，一边理解业务，一边理解这个项目是怎样把 DDD 分层落到 Spring Boot 工程里的。

如果你刚开始学 DDD，最重要的一点是先建立一个朴素认识：

- `interfaces` 层负责“接请求、吐响应”
- `application` 层负责“编排用例”
- `domain` 层负责“业务规则与领域对象”
- `infrastructure` 层负责“技术实现与外部系统接入”

这个项目的后端主目录是：

- `AgentX/src/main/java/org/xhy`

主启动类是：

- `AgentX/src/main/java/org/xhy/AgentXApplication.java`

它是一个 Spring Boot 3 + Java 17 + MyBatis-Plus + PostgreSQL + LangChain4j 的后端系统，核心业务围绕以下几个能力展开：

- 用户注册、登录、验证码、GitHub OAuth
- Agent 创建、编辑、发布、工作区管理
- 会话、消息、上下文、流式聊天
- LLM 服务商与模型管理
- Tool 上传、审核、发布、安装
- Task 任务状态管理

## 1. 先从整体结构看项目

后端核心目录结构如下：

```text
AgentX/src/main/java/org/xhy
├─ interfaces
├─ application
├─ domain
└─ infrastructure
```

这是这个项目最值得先建立的脑图。

### 1.1 interfaces 层

路径示例：

- `AgentX/src/main/java/org/xhy/interfaces/api/portal/agent/PortalAgentController.java`
- `AgentX/src/main/java/org/xhy/interfaces/api/portal/user/LoginController.java`
- `AgentX/src/main/java/org/xhy/interfaces/api/portal/tool/PortalToolController.java`
- `AgentX/src/main/java/org/xhy/interfaces/api/portal/llm/PortalLLMController.java`
- `AgentX/src/main/java/org/xhy/interfaces/api/admin/AdminLLMController.java`

这一层的职责很单纯：

- 定义 HTTP API
- 接收请求参数 DTO
- 从 `UserContext` 里拿当前登录用户
- 调用 `application` 层服务
- 把返回结果包装成统一 `Result`

换句话说，这一层**不应该承载复杂业务规则**。如果 Controller 里写了很多 if/else、状态判断、版本校验，那就说明业务逻辑跑偏了。

### 1.2 application 层

路径示例：

- `AgentX/src/main/java/org/xhy/application/agent/service/AgentAppService.java`
- `AgentX/src/main/java/org/xhy/application/conversation/service/ConversationAppService.java`
- `AgentX/src/main/java/org/xhy/application/tool/service/ToolAppService.java`
- `AgentX/src/main/java/org/xhy/application/llm/service/LLMAppService.java`
- `AgentX/src/main/java/org/xhy/application/user/service/LoginAppService.java`

这一层是“用例编排层”。它通常会做这些事：

- 校验请求是否适合进入业务流程
- 调用 Assembler 把 Request 转成 Entity/DTO
- 串联多个 DomainService 完成一条业务链路
- 协调事务边界
- 把领域对象再转成返回 DTO

它和 domain 的边界可以这样理解：

- `application` 关注“这次请求要走哪几个步骤”
- `domain` 关注“每个步骤里面的业务规则是什么”

### 1.3 domain 层

路径示例：

- `AgentX/src/main/java/org/xhy/domain/agent`
- `AgentX/src/main/java/org/xhy/domain/conversation`
- `AgentX/src/main/java/org/xhy/domain/llm`
- `AgentX/src/main/java/org/xhy/domain/tool`
- `AgentX/src/main/java/org/xhy/domain/token`
- `AgentX/src/main/java/org/xhy/domain/user`

这一层是项目的业务核心，主要由下面几类对象构成：

- `model`：领域实体、聚合内对象、配置对象
- `repository`：领域仓储接口，底层仍由 MyBatis-Plus 承接
- `service`：领域服务，放跨实体、跨仓储的业务规则
- `constant` / `enums`：领域内状态、类型、枚举
- `factory`：领域对象构造辅助

这个项目的 DDD 并不是那种“每个聚合都极度纯粹”的教科书式写法，而是偏工程化、可落地的分层式 DDD。也就是说：

- 领域层里仍然有一些偏数据访问协调的代码
- 实体里封装了一部分行为，但没有做到“所有规则都进实体”
- 大量真实业务逻辑还是落在 `DomainService`

这很常见，也很适合中大型 Spring 项目。

### 1.4 infrastructure 层

路径示例：

- `AgentX/src/main/java/org/xhy/infrastructure/auth`
- `AgentX/src/main/java/org/xhy/infrastructure/config`
- `AgentX/src/main/java/org/xhy/infrastructure/llm`
- `AgentX/src/main/java/org/xhy/infrastructure/github`
- `AgentX/src/main/java/org/xhy/infrastructure/mcp_gateway`
- `AgentX/src/main/java/org/xhy/infrastructure/email`

这一层承担技术细节：

- JWT 鉴权
- Spring MVC / MyBatis 配置
- 邮件发送
- GitHub API 接入
- MCP Gateway 调用
- LangChain4j 模型客户端创建
- MyBatis 类型转换器与 TypeHandler
- SSE 传输

DDD 并不是不要技术实现，而是把这些技术细节收拢到 `infrastructure`，避免污染核心业务。

## 2. 运行入口与基础配置

### 2.1 启动入口

- `AgentX/src/main/java/org/xhy/AgentXApplication.java`

它只是标准的 Spring Boot 启动类，没有塞业务逻辑，这符合分层原则。

### 2.2 核心配置文件

- `AgentX/src/main/resources/application.yml`

从配置可以看出几个关键信息：

- 服务端口默认 `8080`
- 接口上下文路径是 `/api`
- 数据库是 PostgreSQL
- 使用了 MyBatis-Plus 逻辑删除
- 配了 SMTP 邮件发送
- 配了 GitHub OAuth
- 配了 MCP Gateway 地址

也就是说，前端实际访问的大多数接口前缀会是：

```text
/api/...
```

## 3. 建立一张总调用链脑图

在这个项目里，最常见的一条链路是：

```text
Controller
  -> AppService
  -> DomainService
  -> Repository / Infrastructure Service
  -> 返回 DTO
```

以不同模块为例：

- 用户登录：`LoginController -> LoginAppService -> UserDomainService -> UserRepository`
- Agent 管理：`PortalAgentController -> AgentAppService -> AgentDomainService -> AgentRepository`
- 聊天：`ConversationAppService -> MessageHandlerFactory -> AbstractMessageHandler -> LLMServiceFactory`
- Tool 发布：`ToolAppService -> ToolDomainService / ToolVersionDomainService -> ToolStateService -> GitHubService / MCPGatewayService`
- 模型管理：`PortalLLMController -> LLMAppService -> LLMDomainService -> ProviderRepository / ModelRepository`

你读源码时，始终带着一个问题：

“现在这个类是在处理请求、编排流程、执行业务规则，还是做技术接入？”

这个问题会帮你快速定位它属于哪一层。

## 4. 用户模块怎么读

用户相关关键路径如下：

- Controller
  - `AgentX/src/main/java/org/xhy/interfaces/api/portal/user/LoginController.java`
  - `AgentX/src/main/java/org/xhy/interfaces/api/portal/user/PortalUserController.java`
  - `AgentX/src/main/java/org/xhy/interfaces/api/portal/user/OAuthController.java`
- Application
  - `AgentX/src/main/java/org/xhy/application/user/service/LoginAppService.java`
  - `AgentX/src/main/java/org/xhy/application/user/service/UserAppService.java`
  - `AgentX/src/main/java/org/xhy/application/user/service/OAuthAppService.java`
  - `AgentX/src/main/java/org/xhy/application/user/assembler/UserAssembler.java`
- Domain
  - `AgentX/src/main/java/org/xhy/domain/user/service/UserDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/user/model/UserEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/user/repository/UserRepository.java`
- Infrastructure
  - `AgentX/src/main/java/org/xhy/infrastructure/auth/UserAuthInterceptor.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/auth/UserContext.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/email/EmailService.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/verification/VerificationCodeService.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/verification/CaptchaUtils.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/utils/JwtUtils.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/utils/PasswordUtils.java`

### 4.1 LoginController 在做什么

`LoginController` 是登录注册入口，它暴露了这些接口：

- `/login`
- `/register`
- `/get-captcha`
- `/send-email-code`
- `/send-reset-password-code`
- `/verify-email-code`
- `/reset-password`

它只做两件事：

- 接收请求
- 调 `LoginAppService`

这说明用户认证主流程并没有被 Controller 污染。

### 4.2 LoginAppService 在做什么

- `AgentX/src/main/java/org/xhy/application/user/service/LoginAppService.java`

这是典型的应用服务。它负责：

- 登录成功后生成 JWT
- 注册前决定是否要校验邮箱验证码
- 发送注册验证码或重置密码验证码
- 重置密码前做验证码校验

它依赖的下游包括：

- `UserDomainService`
- `EmailService`
- `VerificationCodeService`
- `JwtUtils`

所以你可以把它理解成“用户认证用例总调度器”。

### 4.3 UserDomainService 在做什么

- `AgentX/src/main/java/org/xhy/domain/user/service/UserDomainService.java`

这里放的是更贴近业务规则的逻辑：

- 根据邮箱/手机号查用户
- 注册时加密密码
- 登录时校验密码
- 检查账号是否重复
- 更新密码

其中比较典型的规则包括：

- 注册前先检查账号是否存在
- 登录必须通过密码匹配
- 密码更新必须先加载用户

这类“规则判断”就更适合放在领域层。

### 4.4 鉴权链路

请求进来后，`UserAuthInterceptor` 会：

1. 从 `Authorization` 里拿 Bearer Token
2. 用 `JwtUtils` 校验
3. 解析出用户 ID
4. 写入 `UserContext`
5. 请求结束后清理 `UserContext`

关键文件：

- `AgentX/src/main/java/org/xhy/infrastructure/auth/UserAuthInterceptor.java`
- `AgentX/src/main/java/org/xhy/infrastructure/auth/UserContext.java`

这是一种很典型的 Web 鉴权基础设施设计：认证逻辑不进业务层，而是先通过拦截器把“当前用户”准备好。

## 5. Agent 模块怎么读

关键路径如下：

- Controller
  - `AgentX/src/main/java/org/xhy/interfaces/api/portal/agent/PortalAgentController.java`
  - `AgentX/src/main/java/org/xhy/interfaces/api/portal/agent/PortalWorkspaceController.java`
  - `AgentX/src/main/java/org/xhy/interfaces/api/portal/agent/PortalAgentSessionController.java`
  - `AgentX/src/main/java/org/xhy/interfaces/api/admin/AdminAgentController.java`
- Application
  - `AgentX/src/main/java/org/xhy/application/agent/service/AgentAppService.java`
  - `AgentX/src/main/java/org/xhy/application/agent/service/AgentWorkspaceAppService.java`
  - `AgentX/src/main/java/org/xhy/application/agent/service/AgentSessionAppService.java`
  - `AgentX/src/main/java/org/xhy/application/agent/assembler/AgentAssembler.java`
  - `AgentX/src/main/java/org/xhy/application/agent/assembler/AgentVersionAssembler.java`
- Domain
  - `AgentX/src/main/java/org/xhy/domain/agent/service/AgentDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/agent/service/AgentWorkspaceDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/agent/service/AgentValidator.java`
  - `AgentX/src/main/java/org/xhy/domain/agent/service/ModelProviderFacade.java`
  - `AgentX/src/main/java/org/xhy/domain/agent/model/AgentEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/agent/model/AgentVersionEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/agent/model/AgentWorkspaceEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/agent/model/LLMModelConfig.java`
  - `AgentX/src/main/java/org/xhy/domain/agent/repository/AgentRepository.java`
  - `AgentX/src/main/java/org/xhy/domain/agent/repository/AgentVersionRepository.java`
  - `AgentX/src/main/java/org/xhy/domain/agent/repository/AgentWorkspaceRepository.java`

### 5.1 AgentEntity 是什么

- `AgentX/src/main/java/org/xhy/domain/agent/model/AgentEntity.java`

这是 Agent 聚合里最核心的实体，承载的数据包括：

- 基础信息：名称、头像、描述
- 行为配置：`systemPrompt`、`welcomeMessage`
- 能力配置：`tools`、`knowledgeBaseIds`
- 发布关系：`publishedVersion`
- 状态信息：`enabled`
- 所属关系：`userId`

这个实体还封装了几个行为方法：

- `enable()`
- `disable()`
- `publishVersion()`
- `updateBasicInfo()`
- `updateConfig()`

说明它不是纯 DTO，而是带了一部分状态变化能力的领域实体。

### 5.2 AgentAppService 在做什么

- `AgentX/src/main/java/org/xhy/application/agent/service/AgentAppService.java`

它的职责很清楚：

- 创建 Agent
- 创建默认工作区 `AgentWorkspaceEntity`
- 查询用户 Agent 列表
- 更新 Agent
- 发布 Agent 版本
- 查询版本列表

例如创建 Agent 的调用链大致是：

```text
PortalAgentController.createAgent
  -> AgentAppService.createAgent
  -> AgentAssembler.toEntity
  -> AgentDomainService.createAgent
  -> AgentWorkspaceDomainService.save
```

这里有一个很重要的设计点：**创建 Agent 和创建 Workspace 是同一条应用层用例里的两个步骤**。这说明 Workspace 不是随便的附属表，而是 Agent 运行配置的一部分。

### 5.3 AgentDomainService 在做什么

- `AgentX/src/main/java/org/xhy/domain/agent/service/AgentDomainService.java`

它承担了 Agent 核心规则：

- 根据 `agentId + userId` 查用户自己的 Agent
- 更新 Agent
- 切换启用/禁用
- 删除 Agent 及其版本
- 发布版本
- 校验版本号必须递增
- 获取已发布版本
- 基于发布版本回填 Agent 数据

最值得你注意的是两个点：

1. 它把“发布版本号必须变大”这种规则放在了领域层，而不是 Controller
2. 它把“已发布版本覆盖 Agent 展示内容”的逻辑也放在领域层

这就是 DDD 项目里常见的“规则归领域层”。

## 6. 会话与聊天模块怎么读

这是整个项目最值得精读的模块之一。

关键路径如下：

- Controller
  - 会话控制器主要在 `PortalAgentSessionController.java`
- Application
  - `AgentX/src/main/java/org/xhy/application/conversation/service/ConversationAppService.java`
  - `AgentX/src/main/java/org/xhy/application/conversation/service/handler/MessageHandlerFactory.java`
  - `AgentX/src/main/java/org/xhy/application/conversation/service/message/AbstractMessageHandler.java`
  - `AgentX/src/main/java/org/xhy/application/conversation/service/message/chat/ChatMessageHandler.java`
  - `AgentX/src/main/java/org/xhy/application/conversation/service/message/agent/AgentMessageHandler.java`
  - `AgentX/src/main/java/org/xhy/application/conversation/service/message/agent/AgentToolManager.java`
  - `AgentX/src/main/java/org/xhy/application/conversation/service/handler/context/ChatContext.java`
  - `AgentX/src/main/java/org/xhy/application/conversation/service/handler/context/AgentPromptTemplates.java`
- Domain
  - `AgentX/src/main/java/org/xhy/domain/conversation/service/ConversationDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/conversation/service/SessionDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/conversation/service/MessageDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/conversation/service/ContextDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/conversation/service/ContextProcessor.java`
  - `AgentX/src/main/java/org/xhy/domain/conversation/service/ChatCompletionHandler.java`
  - `AgentX/src/main/java/org/xhy/domain/conversation/service/impl/ChatCompletionHandlerImpl.java`
  - `AgentX/src/main/java/org/xhy/domain/conversation/model/SessionEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/conversation/model/MessageEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/conversation/model/ContextEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/conversation/factory/MessageFactory.java`
- Infrastructure
  - `AgentX/src/main/java/org/xhy/infrastructure/transport/MessageTransport.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/transport/SseMessageTransport.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/transport/MessageTransportFactory.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/sse/SseEmitterFactory.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/llm/LLMServiceFactory.java`

### 6.1 ConversationAppService 是聊天总入口

`ConversationAppService` 的核心职责不是“直接调模型”，而是“先把一切聊天环境准备完整”。

它主要做这些事：

1. 根据 `sessionId` 找会话
2. 根据会话拿到 `agentId`
3. 加载 Agent
4. 加载用户工作区中的模型配置 `LLMModelConfig`
5. 加载模型 `ModelEntity`
6. 加载服务商 `ProviderEntity`
7. 加载上下文 `ContextEntity`
8. 加载历史消息 `MessageEntity`
9. 应用 token 溢出策略
10. 选择消息处理器并启动流式聊天

这说明它是一个标准的应用层编排器。

### 6.2 MessageHandlerFactory 是策略分发点

- `AgentX/src/main/java/org/xhy/application/conversation/service/handler/MessageHandlerFactory.java`

它根据 `agent.getAgentType()` 选择处理器：

- 标准聊天 Agent -> `chatMessageHandler`
- 带工具能力的 Agent -> `agentMessageHandler`

这是一种很典型的“按类型选择处理实现”的工厂模式。

### 6.3 AbstractMessageHandler 是模板方法核心

- `AgentX/src/main/java/org/xhy/application/conversation/service/message/AbstractMessageHandler.java`

这是聊天模块最关键的类之一。它定义了一套通用流程：

1. 创建 SSE 连接
2. 通过 `LLMServiceFactory` 创建流式模型客户端
3. 创建用户消息和 AI 消息实体
4. 保存用户消息并更新上下文
5. 初始化聊天记忆
6. 组装历史消息与 system prompt
7. 由子类决定是否提供工具
8. 构造 LangChain4j Agent
9. 处理 partial response / complete response / tool executed

这就是模板方法模式：

- 父类决定流程骨架
- 子类只补差异点

### 6.4 ChatMessageHandler 与 AgentMessageHandler 的区别

- `ChatMessageHandler`：普通聊天，不挂工具
- `AgentMessageHandler`：通过 `AgentToolManager` 提供 `ToolProvider`

对应文件：

- `AgentX/src/main/java/org/xhy/application/conversation/service/message/chat/ChatMessageHandler.java`
- `AgentX/src/main/java/org/xhy/application/conversation/service/message/agent/AgentMessageHandler.java`

这说明“聊天”和“可调用工具的 Agent”共享一套主流程，但在工具能力上分叉。

### 6.5 聊天链路怎么走

建议你把这条链背下来：

```text
前端发起聊天
  -> ConversationAppService.chat
  -> prepareEnvironment
  -> MessageHandlerFactory.getHandler
  -> AbstractMessageHandler.chat
  -> LLMServiceFactory.getStreamingClient
  -> TokenStream 回调
  -> MessageDomainService 保存消息
  -> SSE 持续推流给前端
```

这条链一旦理解清楚，整个聊天系统就通了。

## 7. Token 上下文裁剪模块怎么读

关键路径：

- `AgentX/src/main/java/org/xhy/domain/token/service/TokenDomainService.java`
- `AgentX/src/main/java/org/xhy/domain/token/service/TokenOverflowStrategy.java`
- `AgentX/src/main/java/org/xhy/domain/token/service/TokenOverflowStrategyFactory.java`
- `AgentX/src/main/java/org/xhy/domain/token/service/impl/NoTokenOverflowStrategy.java`
- `AgentX/src/main/java/org/xhy/domain/token/service/impl/SlidingWindowTokenOverflowStrategy.java`
- `AgentX/src/main/java/org/xhy/domain/token/service/impl/SummarizeTokenOverflowStrategy.java`
- `AgentX/src/main/java/org/xhy/domain/token/model/TokenMessage.java`
- `AgentX/src/main/java/org/xhy/domain/token/model/TokenProcessResult.java`
- `AgentX/src/main/java/org/xhy/domain/token/model/config/TokenOverflowConfig.java`
- `AgentX/src/main/java/org/xhy/domain/shared/enums/TokenOverflowStrategyEnum.java`

这个模块很适合拿来学习策略模式。

它的设计意图是：

- 会话历史太长时，不同模型需要不同的上下文裁剪方案
- 所以把“怎么裁”抽象成 `TokenOverflowStrategy`
- 再由 `Factory` 根据配置选择具体实现

几个策略含义：

- `NoTokenOverflowStrategy`：不裁剪
- `SlidingWindowTokenOverflowStrategy`：滑动窗口保留最近消息
- `SummarizeTokenOverflowStrategy`：把旧消息摘要化

而 `ConversationAppService` 会在组装聊天环境时调用这套策略，决定哪些历史消息继续保留在 `ContextEntity.activeMessages` 中。

## 8. LLM 模块怎么读

关键路径如下：

- Controller
  - `AgentX/src/main/java/org/xhy/interfaces/api/portal/llm/PortalLLMController.java`
  - `AgentX/src/main/java/org/xhy/interfaces/api/admin/AdminLLMController.java`
- Application
  - `AgentX/src/main/java/org/xhy/application/llm/service/LLMAppService.java`
  - `AgentX/src/main/java/org/xhy/application/admin/llm/service/AdminLLMAppService.java`
  - `AgentX/src/main/java/org/xhy/application/llm/assembler/ProviderAssembler.java`
  - `AgentX/src/main/java/org/xhy/application/llm/assembler/ModelAssembler.java`
- Domain
  - `AgentX/src/main/java/org/xhy/domain/llm/service/LLMDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/llm/service/LLMRequestService.java`
  - `AgentX/src/main/java/org/xhy/domain/llm/model/ProviderEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/llm/model/ProviderAggregate.java`
  - `AgentX/src/main/java/org/xhy/domain/llm/model/ModelEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/llm/model/LLMRequest.java`
  - `AgentX/src/main/java/org/xhy/domain/llm/repository/ProviderRepository.java`
  - `AgentX/src/main/java/org/xhy/domain/llm/repository/ModelRepository.java`
- Infrastructure
  - `AgentX/src/main/java/org/xhy/infrastructure/llm/LLMServiceFactory.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/llm/LLMProviderService.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/llm/factory/LLMProviderFactory.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/llm/protocol/enums/ProviderProtocol.java`

### 8.1 Provider 与 Model 的关系

这个模块把 LLM 配置拆成两层：

- `ProviderEntity`：服务商，例如 OpenAI、Anthropic、第三方兼容网关
- `ModelEntity`：具体模型，例如某个 `gpt-*` 或 Claude 模型

这种拆法很好理解：

- 一个服务商下可以挂多个模型
- 同一个用户可以有自己的自定义 Provider
- 官方 Provider 和用户自定义 Provider 可以共存

### 8.2 ProviderAggregate 是什么

- `AgentX/src/main/java/org/xhy/domain/llm/model/ProviderAggregate.java`

它把一个 `ProviderEntity` 和它下面的一组 `ModelEntity` 聚合起来，方便应用层一次性拿完整数据。你可以把它理解成“面向业务视图的聚合对象”。

### 8.3 LLMServiceFactory 在做什么

- `AgentX/src/main/java/org/xhy/infrastructure/llm/LLMServiceFactory.java`

它负责把领域层的：

- `ProviderEntity`
- `ModelEntity`

转成基础设施层真正可调用的 LangChain4j 客户端。

这一步非常关键，因为它把“业务配置对象”和“第三方 SDK 客户端”隔开了。

### 8.4 LLMProviderFactory 在做什么

- `AgentX/src/main/java/org/xhy/infrastructure/llm/factory/LLMProviderFactory.java`

这里根据 `ProviderProtocol` 创建不同模型客户端：

- OpenAI 协议
- Anthropic 协议

所以 LLM 扩展点主要在这里。如果以后要接更多模型协议，这里会是一个重要扩展位。

## 9. Tool 模块怎么读

这是另一个很值得细读的模块，因为它明显用了状态模式。

关键路径如下：

- Controller
  - `AgentX/src/main/java/org/xhy/interfaces/api/portal/tool/PortalToolController.java`
  - `AgentX/src/main/java/org/xhy/interfaces/api/admin/AdminToolController.java`
- Application
  - `AgentX/src/main/java/org/xhy/application/tool/service/ToolAppService.java`
  - `AgentX/src/main/java/org/xhy/application/admin/tool/service/AdminToolAppService.java`
  - `AgentX/src/main/java/org/xhy/application/tool/crontab/ToolProcessMonitor.java`
  - `AgentX/src/main/java/org/xhy/application/tool/assembler/ToolAssembler.java`
- Domain
  - `AgentX/src/main/java/org/xhy/domain/tool/service/ToolDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/service/ToolVersionDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/service/UserToolDomainService.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/service/ToolStateService.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/service/state/ToolStateProcessor.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/service/state/impl/WaitingReviewProcessor.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/service/state/impl/GithubUrlValidateProcessor.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/service/state/impl/DeployingProcessor.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/service/state/impl/FetchingToolsProcessor.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/service/state/impl/ManualReviewProcessor.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/service/state/impl/PublishingProcessor.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/model/ToolEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/model/ToolVersionEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/model/UserToolEntity.java`
  - `AgentX/src/main/java/org/xhy/domain/tool/constant/ToolStatus.java`
- Infrastructure
  - `AgentX/src/main/java/org/xhy/infrastructure/github/GitHubService.java`
  - `AgentX/src/main/java/org/xhy/infrastructure/mcp_gateway/MCPGatewayService.java`

### 9.1 三个核心实体分别是什么

- `ToolEntity`：工具自身，类似工具主档
- `ToolVersionEntity`：工具发布版本
- `UserToolEntity`：用户安装到自己空间里的工具副本

你可以这样理解：

- `ToolEntity` 管“我是哪个工具”
- `ToolVersionEntity` 管“这个工具发布过哪些版本”
- `UserToolEntity` 管“某个用户当前装的是哪个版本”

### 9.2 ToolAppService 在做什么

- `AgentX/src/main/java/org/xhy/application/tool/service/ToolAppService.java`

它是工具用例编排层，负责：

- 上传工具
- 查询用户工具
- 修改/删除工具
- 工具上架（生成版本）
- 市场分页查询
- 安装工具
- 卸载工具
- 推荐工具

它的一个典型特点是：会同时协调多个领域服务，例如：

- `ToolDomainService`
- `ToolVersionDomainService`
- `UserToolDomainService`
- `UserDomainService`

这正是 application 层存在的意义。

### 9.3 ToolStateService 是整个 Tool 模块的灵魂

- `AgentX/src/main/java/org/xhy/domain/tool/service/ToolStateService.java`

这是状态流转总控服务。它维护：

- 当前状态对应哪个 `ToolStateProcessor`
- 是否异步处理
- 状态结束后要不要自动跳下一个状态
- 失败时如何标记失败步骤

它做的事非常像一个轻量状态机执行器。

### 9.4 ToolStateProcessor 是状态模式接口

- `AgentX/src/main/java/org/xhy/domain/tool/service/state/ToolStateProcessor.java`

统一定义了三个方法：

- `getStatus()`
- `process(ToolEntity tool)`
- `getNextStatus()`

这意味着每种状态只需要关心：

- 我处理哪个状态
- 这个状态怎么执行
- 执行完应该流到哪里

这就是经典状态模式。

### 9.5 PublishingProcessor 在做什么

- `AgentX/src/main/java/org/xhy/domain/tool/service/state/impl/PublishingProcessor.java`

这个类很值得细看，它负责：

1. 解析源 GitHub 仓库地址
2. 如果 URL 没带版本，则解析最新 commit/ref
3. 下载源码压缩包
4. 解压到临时目录
5. 定位真正的内容根目录
6. 组装目标仓库发布路径
7. 调用 `GitHubService` 提交到目标仓库
8. 清理临时文件

这已经不是“普通表 CRUD”，而是一个真实的业务流程类，所以放在领域状态处理器里是有道理的。

## 10. Task 模块怎么读

关键路径：

- `AgentX/src/main/java/org/xhy/interfaces/api/portal/agent/TaskController.java`
- `AgentX/src/main/java/org/xhy/application/task/service/TaskAppService.java`
- `AgentX/src/main/java/org/xhy/application/task/assembler/TaskAssembler.java`
- `AgentX/src/main/java/org/xhy/domain/task/service/TaskDomainService.java`
- `AgentX/src/main/java/org/xhy/domain/task/model/TaskEntity.java`
- `AgentX/src/main/java/org/xhy/domain/task/model/TaskAggregate.java`
- `AgentX/src/main/java/org/xhy/domain/task/repository/TaskRepository.java`

这个模块相对独立，主要承担任务记录与状态更新。它不像聊天和 Tool 模块那样复杂，但在系统中起到“异步过程结果追踪”的作用。

## 11. 基础设施层有哪些必须认识的类

### 11.1 配置类

- `AgentX/src/main/java/org/xhy/infrastructure/config/WebMvcConfig.java`
- `AgentX/src/main/java/org/xhy/infrastructure/config/WebConfig.java`
- `AgentX/src/main/java/org/xhy/infrastructure/config/MybatisPlusConfig.java`
- `AgentX/src/main/java/org/xhy/infrastructure/config/MyBatisTypeHandlerConfig.java`
- `AgentX/src/main/java/org/xhy/infrastructure/config/GlobalExceptionHandler.java`
- `AgentX/src/main/java/org/xhy/infrastructure/config/GitHubOAuthProperties.java`
- `AgentX/src/main/java/org/xhy/infrastructure/config/GitHubProperties.java`
- `AgentX/src/main/java/org/xhy/infrastructure/config/MCPGatewayProperties.java`

这部分负责把 Web、MyBatis、异常、配置项绑定到 Spring 容器。

### 11.2 基础实体与统一返回

- `AgentX/src/main/java/org/xhy/infrastructure/entity/BaseEntity.java`
- `AgentX/src/main/java/org/xhy/interfaces/api/common/Result.java`

`BaseEntity` 提供共性字段，`Result` 提供统一响应包装。它们虽然简单，但构成了整个工程的统一约定。

### 11.3 异常体系

- `AgentX/src/main/java/org/xhy/infrastructure/exception/BusinessException.java`
- `AgentX/src/main/java/org/xhy/infrastructure/exception/ParamValidationException.java`
- `AgentX/src/main/java/org/xhy/infrastructure/exception/EntityNotFoundException.java`
- `AgentX/src/main/java/org/xhy/infrastructure/config/GlobalExceptionHandler.java`

这是标准的“抛业务异常 -> 统一拦截转换为 HTTP 响应”的设计。

### 11.4 MyBatis 类型转换

关键目录：

- `AgentX/src/main/java/org/xhy/infrastructure/converter`
- `AgentX/src/main/java/org/xhy/infrastructure/typehandler`

这些类的作用是把数据库字段和 Java 对象做桥接，例如：

- 枚举转数据库值
- JSON 字段转对象
- List/Map/复杂配置对象序列化

这在配置型实体很多的系统里很常见，比如：

- `LLMModelConfig`
- `ToolDefinition`
- `ProviderConfig`

## 12. 我建议你的源码阅读顺序

### 路线一：先理解分层

1. `interfaces/api/...Controller`
2. `application/.../service/*AppService`
3. `domain/.../service/*DomainService`
4. `domain/.../model/*Entity`
5. `infrastructure/...`

适合你先建立 DDD 的层次感。

### 路线二：按业务链路读

如果你想最快“读通一个功能”，我建议按这几条线读：

#### 1) 登录注册链

- `interfaces/api/portal/user/LoginController.java`
- `application/user/service/LoginAppService.java`
- `domain/user/service/UserDomainService.java`
- `infrastructure/auth/UserAuthInterceptor.java`
- `infrastructure/utils/JwtUtils.java`

#### 2) 创建 Agent 链

- `interfaces/api/portal/agent/PortalAgentController.java`
- `application/agent/service/AgentAppService.java`
- `domain/agent/service/AgentDomainService.java`
- `domain/agent/model/AgentEntity.java`
- `domain/agent/model/AgentWorkspaceEntity.java`

#### 3) 聊天链

- `application/conversation/service/ConversationAppService.java`
- `application/conversation/service/handler/MessageHandlerFactory.java`
- `application/conversation/service/message/AbstractMessageHandler.java`
- `application/conversation/service/message/chat/ChatMessageHandler.java`
- `application/conversation/service/message/agent/AgentMessageHandler.java`
- `domain/conversation/service/MessageDomainService.java`
- `infrastructure/llm/LLMServiceFactory.java`

#### 4) Tool 发布链

- `application/tool/service/ToolAppService.java`
- `domain/tool/service/ToolStateService.java`
- `domain/tool/service/state/ToolStateProcessor.java`
- `domain/tool/service/state/impl/PublishingProcessor.java`
- `infrastructure/github/GitHubService.java`
- `infrastructure/mcp_gateway/MCPGatewayService.java`

## 13. 用 DDD 视角评价这个项目

这个项目有几个很明显的优点。

### 13.1 分层总体是清楚的

虽然不是极端纯粹的 DDD，但层次是清晰的：

- Controller 不重
- AppService 明显承担用例编排
- DomainService 聚焦业务规则
- Infrastructure 承担外部依赖

这已经比“所有逻辑都堆 ServiceImpl”好很多。

### 13.2 聊天模块设计得比较好

聊天模块用了几种很典型的可扩展设计：

- 工厂模式：`MessageHandlerFactory`
- 模板方法：`AbstractMessageHandler`
- 策略模式：Token overflow strategies
- 传输抽象：`MessageTransport`

这说明作者不是单纯在堆业务代码，而是在主动提炼变化点。

### 13.3 Tool 模块有明显状态机思维

`ToolStateService + ToolStateProcessor` 这一套很适合 DDD 学习者观察，因为它体现了：

- 状态流转不是写死在一个大 if/else 里
- 每个状态有自己的处理器
- 技术动作通过基础设施服务完成

这是比较有工程价值的设计。

### 13.4 这个项目也有“工程化 DDD”的折中

你读的时候也要注意，它并不是“纯战术 DDD 教材代码”：

- 某些领域服务仍然写了不少仓储查询逻辑
- 部分实体偏数据承载，业务行为不算特别浓
- 一些跨聚合协调仍然放在 Service 里，而不是更细的聚合行为中

但这并不算缺点。对于 Spring Boot 业务系统来说，这种折中往往更实用。

## 14. 一张精简版类职责索引

下面这些类，是我认为你应该优先吃透的“第一批核心类”。

- `AgentX/src/main/java/org/xhy/application/user/service/LoginAppService.java`
  - 用户认证用例总入口
- `AgentX/src/main/java/org/xhy/domain/user/service/UserDomainService.java`
  - 用户注册/登录核心规则
- `AgentX/src/main/java/org/xhy/application/agent/service/AgentAppService.java`
  - Agent 用例编排入口
- `AgentX/src/main/java/org/xhy/domain/agent/service/AgentDomainService.java`
  - Agent 核心规则与版本逻辑
- `AgentX/src/main/java/org/xhy/domain/agent/model/AgentEntity.java`
  - Agent 核心实体
- `AgentX/src/main/java/org/xhy/application/conversation/service/ConversationAppService.java`
  - 聊天总编排器
- `AgentX/src/main/java/org/xhy/application/conversation/service/message/AbstractMessageHandler.java`
  - 聊天流程模板
- `AgentX/src/main/java/org/xhy/application/conversation/service/handler/MessageHandlerFactory.java`
  - 聊天模式分发器
- `AgentX/src/main/java/org/xhy/domain/token/service/TokenOverflowStrategyFactory.java`
  - 上下文裁剪策略选择器
- `AgentX/src/main/java/org/xhy/application/llm/service/LLMAppService.java`
  - 模型配置用例入口
- `AgentX/src/main/java/org/xhy/domain/llm/service/LLMDomainService.java`
  - Provider/Model 核心规则
- `AgentX/src/main/java/org/xhy/infrastructure/llm/LLMServiceFactory.java`
  - 业务模型配置到 SDK 客户端的桥
- `AgentX/src/main/java/org/xhy/application/tool/service/ToolAppService.java`
  - 工具业务编排入口
- `AgentX/src/main/java/org/xhy/domain/tool/service/ToolStateService.java`
  - 工具状态机总控
- `AgentX/src/main/java/org/xhy/domain/tool/service/state/impl/PublishingProcessor.java`
  - 工具发布流程关键实现

## 15. 给你一个实际阅读建议

如果你现在刚学 DDD，不要一上来就试图“把整个项目所有类都读完”。最有效的方法是：

1. 先选一条链，比如“登录”或“聊天”
2. 从 Controller 一路点到 AppService
3. 再点到 DomainService
4. 最后再看 Entity、Repository、Infrastructure
5. 每读完一条链，自己画一张调用图

推荐你优先顺序：

1. 登录注册
2. 创建 Agent
3. 聊天主链
4. Token 裁剪策略
5. Tool 状态流转
6. LLM Provider/Model 管理

这样会比从目录树一层层盲读高效得多。

---

如果把这份项目当成 DDD 学习样本，你最值得重点吸收的不是“名词”，而是这三件事：

- 业务入口如何从 Controller 下沉到 AppService
- 规则如何从流程里抽到 DomainService
- 技术接入如何收口到 Infrastructure

只要你把这三件事在 AgentX 里看明白，这个项目的后端骨架你就真的吃进去了。

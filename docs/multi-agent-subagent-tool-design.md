# AgentX 多智能体（Multi-Agent）设计方案 —— 子 Agent 作为工具（与主会话一致）

本文档描述在 AgentX 现有对话与工具架构上，实现“子 Agent 作为工具（Sub‑Agent Tool）”的多智能体方案。根据最新决策：子 Agent 使用与主 Agent 相同的会话（同一个 `sessionId`），消息落库、链路追踪与计费均复用主链路，不做断开或深度限制；如需回显子 Agent 的中间结果，也应回显到同一条 SSE 连接中。

## 目标与非目标

- 目标
- 子 Agent 以“工具”的形式被主 Agent 动态调用，和现有 MCP/内置工具并存。
- 子 Agent 调用使用与主 Agent 一致的会话（相同 `sessionId`），消息持久化，完整进入会话历史。
- 子 Agent 的中间结果与阶段事件可回显到前端（共享同一 SSE 连接）。
- 追踪/日志/计费覆盖整个链路，直接复用主链路上下文（TraceId 等）。
- 不做深度限制或循环防护，交由大模型自行决策（如后续确有需要再引入治理开关）。
- 非目标
  - 不改变现有主 Agent 的入口 API 与业务语义。
  - 不对前端协议做破坏性调整（新增事件类型可向后兼容）。

## 核心思路与总体架构

现有抽象：
- `AbstractMessageHandler` 模板方法负责任务编排（余额检查、历史拼接、工具注入、流式/同步、计费与钩子）。
- 工具体系由两条链路组成：
  - “内置工具”通过 `BuiltInToolRegistry` 注入（按 Agent 适配）。
  - “动态工具”通过 `ToolProvider` 注入（当前用于 MCP）。

本方案在“动态工具”链路新增一个 `MultiAgentToolProvider`，用于对当前 Agent 配置的“关联 Agent 列表”生成一组工具（每个子 Agent 一个工具）。主 Agent 在流式对话中触发这些工具时，直接在“同一会话”中调用子 Agent 的对话能力，保存消息、复用追踪上下文与计费；同时以事件方式将关键阶段回显给前端，并将工具返回值提供给主模型进行后续回答。

### 关键组件

1) `MultiAgentToolProvider`（新增）
- 职责：
  - 根据主 Agent 的“关联 Agent”配置生成工具定义（每个子 Agent 暴露一个 `call_<agentName>` 工具）。
  - 在工具执行时，进行“临时会话”子调用（使用同步对话能力），收集结果与阶段事件，返回结果给主 Agent。
- 接口：实现 `dev.langchain4j.service.tool.ToolProvider`（或复用 `BuiltInToolProvider` 的风格提供 `Map<ToolSpecification, ToolExecutor>`）。
- 注入：由 `AgentToolManager.createToolProvider(...)` 组合到现有 MCP 工具提供者中，形成 Composite ToolProvider。

2) 子调用执行器（保持同会话）
- 封装对话执行（优先使用流式处理），但将输出桥接到“主 SSE 连接”：
  - 进入子 Agent 执行前，在数据库中保存“子 Agent 用户消息”（`Role.USER`，内容为工具 `message` 参数），`sessionId` 与主会话一致；
  - 由 `MessageHandlerFactory` 选择合适的 `AbstractMessageHandler` 子类（Agent/RAG 等）进行流式对话；
  - 在 Token 流回调中，使用“嵌套传输层（NestedTransport）”将片段以 `SUB_AGENT_*` 事件回显到主 SSE；同时累积文本用于工具返回；
  - 子 Agent 生成结束后，保存子 Agent 的 AI 消息（`Role.ASSISTANT`）到同一 `sessionId`；
  - 工具返回值为子 Agent 的最终文本。
- 记忆与历史：完全复用会话策略（摘要/记忆注入/溢出策略照常生效）。
- 计费：复用 `performBillingWithErrorHandling(...)`；计费主体为当前用户（`userId`）。

3) 事件回显适配层（增强）
- 在工具执行前后与子调用关键阶段，向主连接发送事件：
  - `SUB_AGENT_CALL_START`：包含目标子 Agent 基本信息、入参摘要。
  - `SUB_AGENT_PARTIAL`：子 Agent 的流式片段（可按频率聚合）。
  - `SUB_AGENT_COMPLETE`：子 Agent 的最终结果、token 统计、耗时。
  - `SUB_AGENT_ERROR`：子 Agent 调用错误与简要栈。
- 事件与 `MessageType`：新增一组枚举常量（后向兼容），前端按需渲染。

## 会话一致性设计（与主会话相同）

需求：子 Agent 调用与主 Agent 保持同一会话，消息持久化并进入会话历史，以便：
- 链路追踪复用（同一 Trace 上下文）；
- 历史对话统一治理（摘要、溢出策略、记忆抽取/注入一致）；
- 对话回放与审计一致。

实现要点：
- 使用主会话的 `sessionId` 直接进行子调用，沿用 `ChatContext` 的模型/服务商与高可用策略（子 Agent 自己的配置优先生效）。
- 子调用的消息对（用户/AI）照常落库，`messageDomainService.saveMessageAndUpdateContext(...)` 与摘要/溢出策略按现有逻辑执行。
- 若需要区分子 Agent 的消息，建议在 `MessageType` 上增加 `SUB_AGENT_USER` / `SUB_AGENT_AI`（不破坏现有 TEXT 流程，作为可选增强）。

## 追踪 / 日志 / 计费（全链路、复用）

- 计费：
  - 子 Agent 调用沿用主会话 `userId` 计费；
  - 子调用完成后，按输入/输出 Token 调用 `billingService.charge(...)`（与主链路一致）。

- 追踪：
  - 复用同一 `TraceContext`：在 `MultiAgentToolProvider` 触发子调用前，读取当前 `TraceContext` 并设置到子调用 `ChatContext`；
  - 由 `TracingMessageHandler` 持续记录模型调用、工具调用、完成/失败状态；
  - 如需体现“同会话内的子阶段”，可在 Trace 记录中增加阶段标签（如 sub_agent:<id>），但无需新建 parent/child Trace。

- 日志：
  - 子调用开始、完成/失败、token 用量、耗时、工具名与目标 Agent 信息全部记录到日志（`INFO/DEBUG`）与 TraceCollector。

## 中间结果回显（前端体验，共享连接）

需求：在主流式会话中可见子 Agent 的阶段性输出。

实现策略：
- 使用“嵌套传输层（NestedTransport）”将子 Agent 的 Token 流回调转译为 `SUB_AGENT_*` 事件发往主 SSE；
- 默认采用“聚合片段”的策略（控制频率），也支持配置为“直通片段”以获得更强实时性；
- 事件包含最小必要信息（子 Agent 标识、片段文本、时间戳、统计）。

## API/模型/数据变更

- 新增/变更模型
  - `MessageType`：新增 `SUB_AGENT_CALL_START`、`SUB_AGENT_PARTIAL`、`SUB_AGENT_COMPLETE`、`SUB_AGENT_ERROR`（可选再增加 `SUB_AGENT_USER` / `SUB_AGENT_AI`）。
  - `TraceContext`：无需新增 parent/child 字段，复用同一上下文；如需标签化标识子阶段，扩展记录结构即可。

- Agent 关联配置
  - 建议新增 `AgentEntity.linkedAgentIds: List<String>`（数据库迁移），或短期复用 `toolPresetParams` 存关联清单（权宜）。
  - `AgentToolManager` 读取关联清单，组装 `MultiAgentToolProvider`。

## 安全与治理（按需）

- 不做深度限制与循环检测；模型自行决策调用链条。
- 基础校验仍保留：仅允许调用“关联清单”中的 Agent（或同工作区授权）。
- 如后续出现滥用或成本风险，再增配治理开关（深度/超时/熔断等）。

## 实现步骤（里程碑）

1) 数据与配置
- [可选] 为 `AgentEntity` 增加 `linkedAgentIds` 与迁移脚本；管理端提供配置 UI。

2) Provider 与同会话子调用
- 通过内置工具提供者提供 Multi‑Agent 工具：
  - 新增 `MultiAgentBuiltInToolProvider`（@BuiltInTool）：
    - 工具定义：为每个关联 Agent 生成 `call_<agentName>`，参数 `{ message: string (required) }`；
    - 执行：后续接入同会话流式子调用、事件回显与持久化（当前为占位实现）。
- MCP 工具仍由 `AgentToolManager.createToolProvider(...)` 返回；内置工具与外部 ToolProvider 在 `buildStreamingAgent(...)` 中汇合。

3) Handler/Trace 适配
- 直接复用现有 `AbstractMessageHandler`/`TracingMessageHandler` 逻辑；
- 在子调用前，将当前 `TraceContext` 设置到子调用的 `ChatContext`，保证沿用同一追踪上下文；
- 如需在 Trace 里区分子阶段，可在 `TraceCollector` 中增加阶段标签记录（非必需）。

4) 事件与前端
- 扩充 `MessageType` 并在 SSE 发送 `SUB_AGENT_*` 事件；
- 前端对 `SUB_AGENT_*` 增量渲染（折叠卡片/步骤流）。

5) 保护与治理（可选后续）
- 增加全链路超时/熔断（如出现异常循环高耗时场景）；
- 增加告警与统计（子调用成功率、耗时分布、成本归集）。

## 关键权衡

- 统一会话：消息与追踪完全一致，回放、审计与记忆策略最简单；成本上子调用会增加会话历史长度，可由溢出/摘要策略治理。
- 片段回显粒度：默认聚合片段，平衡实时性与可读性；需要强实时可配置“直通片段”。
- 处理器选择：子 Agent 可使用与自身绑定的模型/服务商与工具；主/子在同会话下共存，不会互相覆盖。

## 未决问题（待确认）

- 子 Agent 历史范围：是否直接使用“同会话全部有效历史”，或限制窗口大小（交由现有 Token 溢出策略处理，默认为后者）。
- 回显粒度：是否提供 Agent 级别开关，从“聚合片段”切换为“直通片段”。
- 失败策略：子调用失败时，是让主 Agent 继续（无工具结果）还是直接中断？建议提供配置项。

---

如认可上述方向，我可以先按“最小可用”实现：MultiAgentToolProvider + 嵌套传输层（转发 `SUB_AGENT_*` 事件）+ 同会话持久化 + 计费/追踪复用，然后补齐可选的片段直通与 Trace 标签化。

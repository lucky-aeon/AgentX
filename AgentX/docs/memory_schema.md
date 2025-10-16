# 记忆系统表结构设计（仅表意与字段说明）

目标：定义“长期记忆”相关的数据库表及字段语义，暂不涉及索引、向量插件或实现细节，便于先评审含义与边界。

## 表名与用途
- `memory_items`（记忆条目元数据表）
  - 用途：保存“可长期复用的信息”的元数据与治理信息，例如用户偏好、目标、稳定事实等。
  - 角色：写入判定后的唯一可信源，提供审计字段（来源、版本、状态、TTL）。

- `memory_vector_store`（记忆向量表）
  - 用途：保存与 `memory_items` 对应的文本向量与必要的冗余信息，用于后续相似度召回。
  - 说明：此表仅定义列含义，暂不绑定具体向量类型或插件（后续可用 pgvector、Milvus 等）。

---

## 字段定义：memory_items（记忆条目元数据表）
- `id`（UUID，主键）
  - 含义：记忆条目唯一标识。
  - 备注：用于与向量表进行关联。
- `user_id`（VARCHAR(64)，必填）
  - 含义：归属用户ID（多租户/多用户隔离）。
- `type`（VARCHAR(16)，必填）
  - 含义：记忆类型，用于分类治理与读取排序。
  - 取值建议：`PROFILE`（偏好/人格）、`TASK`（目标/任务上下文）、`FACT`（稳定事实）、`EPISODIC`（情景/阶段性）。
- `text`（TEXT，必填）
  - 含义：记忆的原文/要点，面向审计与直出展示。
- `data`（JSONB，可选）
  - 含义：结构化负载（如 key/value、实体ID、别名等）。
- `importance`（REAL，默认 0.5）
  - 含义：重要性评分（0–1），用于写入阈值与读取加权。
- `tags`（TEXT[]，默认空）
  - 含义：标签，便于筛选（如 `tone`、`project`）。
- `source_session_id`（VARCHAR(64)，可选）
  - 含义：来源会话ID，便于追溯产生背景。
- `dedupe_hash`（VARCHAR(128)，可选）
  - 含义：语义去重哈希（与 `user_id` 共同限定），实现幂等写入或合并策略。
- `status`（SMALLINT，默认 1）
  - 含义：状态标记；建议：`1=active`、`0=archived/deleted`。
- `created_at`（TIMESTAMPTZ，默认 now()）
  - 含义：创建时间。
- `updated_at`（TIMESTAMPTZ，默认 now()）
  - 含义：最后更新时间。

示例 DDL（不含索引）：
```sql
CREATE TABLE IF NOT EXISTS public.memory_items (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            VARCHAR(64) NOT NULL,
    type               VARCHAR(16) NOT NULL,
    text               TEXT NOT NULL,
    data               JSONB,
    importance         REAL NOT NULL DEFAULT 0.5,
    tags               TEXT[] DEFAULT '{}',
    source_session_id  VARCHAR(64),
    dedupe_hash        VARCHAR(128),
    status             SMALLINT NOT NULL DEFAULT 1,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## 字段定义：memory_vector_store（记忆向量表）
- `embedding_id`（UUID，主键）
  - 含义：向量记录唯一标识。
- `item_id`（UUID，必填，外键到 `memory_items.id`）
  - 含义：与记忆条目关联；删除条目时此处可级联删除。
- `embedding`（JSONB，必填）
  - 含义：向量数据的占位字段（数组形式存储，如 `{"vector":[0.1,0.2,...]}`）。
  - 说明：此处暂不绑定具体向量类型/插件；后续可替换为专用向量类型（如 pgvector 的 `VECTOR(n)`）。
- `text`（TEXT，必填）
  - 含义：与条目同步的冗余文本，便于快速展示/调试（与 `memory_items.text` 保持一致）。
- `metadata`（JSONB，默认空对象）
  - 含义：轻量冗余（如 `{"type":"PROFILE","tags":[...],"user_id":"..."}`），便于不联表过滤。
- `created_at`（TIMESTAMPTZ，默认 now()）
  - 含义：创建时间。

示例 DDL（不含索引、无插件约束）：
```sql
CREATE TABLE IF NOT EXISTS public.memory_vector_store (
    embedding_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id        UUID NOT NULL REFERENCES public.memory_items(id) ON DELETE CASCADE,
    embedding      JSONB NOT NULL,
    text           TEXT NOT NULL,
    metadata       JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

---

## 技术方案（如何存储、如何使用）

本节描述不依赖任何特定向量插件的最小实现路径，便于先行落地与验证；后续可无感替换为任意向量引擎。

### 写入（记忆存储）
- 触发时机：每轮对话结束后（推荐在应用的对话完成钩子执行）。
- 流程：
  1) 抽取：对“本轮 user 问 + assistant 答”进行小模型抽取，产出候选记忆项（JSON 数组）。
  2) 评估：为每条候选打重要性分；低于阈值丢弃；生成 `dedupe_hash` 与同用户历史比对，决定“新增/合并/忽略”。
  3) 入库（memory_items）：保存通过评估的记忆条目（`user_id,type,text,data,importance,tags,source_session_id,dedupe_hash,status=1`）。
  4) 生成向量（应用层）：调用嵌入模型将 `text` 转为向量（数组），以 JSON 形式写入 `memory_vector_store.embedding`；同时写入 `item_id,text,metadata`（可将 `type,tags,user_id` 复制入 `metadata` 便于快速筛选）。

- 候选记忆输出示例（抽取器返回）：
  ```json
  [
    {"type":"PROFILE","text":"更喜欢简洁直接的回答","importance":0.8,"tags":["tone"],"source_session_id":"sess_123"},
    {"type":"TASK","text":"正在实现 AgentX 记忆 MVP","importance":0.7,"tags":["project"],"source_session_id":"sess_123"}
  ]
  ```

- 去重策略建议：
  - `dedupe_hash = hash(normalize(text))`（可用 SimHash/MinHash 或简化的哈希）；入库前按 `(user_id,dedupe_hash)` 查重；如需合并则更新 `text/tags/importance`。

### 读取（记忆使用）
- 触发时机：对话上下文构建阶段（在系统提示词注入前/后）。
- 流程（无插件版）：
  1) 候选集：按 `user_id` 联表 `memory_items` 与 `memory_vector_store` 获取近期 N 条（例如 300–500 条）active 记忆及其向量；
  2) 应用层相似度：对当前用户输入做嵌入，计算与候选集的相似度（cosine/内积），取 Top-K（3–8）；
  3) 压缩与注入：将 Top-K 的 `text` 压缩成要点，合成为一段“记忆背景”插入到系统提示词中（或作为单独一条 SystemMessage 置于历史前部）。

- 伪代码（读取注入）：
  ```pseudo
  // before buildHistoryMessage system prompt
  candidates = queryRecentActiveMemories(userId, limit=500)  // join items + vector_store
  queryVec = embed(userInput)
  scored = cosineSimilarity(queryVec, candidates.embeddings)
  topK = selectTopK(scored, k=5)
  summary = compress(topK.texts)   // 生成 3–6 条要点
  systemPrompt = baseSystemPrompt + "\n[记忆要点]\n" + summary
  ```

### 接入位置（示例，便于代码落点）
- 写入：对话完成钩子（如 `AbstractMessageHandler.onChatCompleted(...)`）。
- 读取：上下文构建处（如 `AbstractMessageHandler.buildHistoryMessage(...)`，在拼接系统提示词附近）。

### 简要 API 约定
- 保存记忆（服务层）：`saveMemories(userId, sessionId, List<CandidateMemory>)` → 返回写入的 `item_id` 列表。
- 检索记忆（服务层）：`searchMemories(userId, queryText, topK)` → 返回 Top-K 记忆要点（文本 + 元信息）。

### 说明
- 当前设计使用 JSONB 存向量，便于先跑通流程；后续可平滑替换为任意向量类型/引擎，并保持表/字段名不变。
```

---

## 表之间关系
- 一般为 1:1（一个记忆条目对应一条向量记录）。
- 如需分片或多切片策略，可扩展为 1:N（多个向量片段指向同一 `item_id`）。

---

## 不在本次范围
- 索引设计（BTree/GiST/GiN/向量索引等）。
- 向量插件/引擎选型与落地细节（pgvector、Milvus、Qdrant 等）。
- 读写流程、抽取与评估逻辑实现。

---

## 评审通过后的下一步（仅说明）
1) 提交迁移脚本（Flyway）。
2) 新增 Memory Embedding 存储 Bean（与知识库向量库并存）。
3) 接入读取路径（对话前注入 Top-K 记忆要点）。
4) 接入写入路径（对话后抽取→去重→入库）。

---

## 技术方案（与现有代码对齐）

本方案直接融入当前项目结构与组件，避免后续二次改造。

### 1) 配置与 Bean（第二个向量存储 Bean）
- 新增配置前缀：`memory.embedding.vector-store.*`（与现有 `embedding.vector-store.*` 一致字段，单独指向记忆表）。
- 新增类：`src/main/java/org/xhy/infrastructure/memory/config/MemoryEmbeddingConfig.java`
  - 提供 Bean：`@Bean @Qualifier("memoryEmbeddingStore") EmbeddingStore<TextSegment>`
  - 构建方式与 `EmbeddingConfig.initEmbeddingStore()` 相同，但 `table` 使用 `memory.embedding.vector-store.table`（建议默认 `public.memory_vector_store`）。
- 新增配置类：`MemoryEmbeddingProperties`（prefix=`memory.embedding`），字段同 `EmbeddingProperties`。

示例 application.yml 片段（仅示意）：
```
memory:
  embedding:
    vector-store:
      host: ${VECTOR_DB_HOST}
      port: ${VECTOR_DB_PORT}
      user: ${VECTOR_DB_USER}
      password: ${VECTOR_DB_PASSWORD}
      database: ${VECTOR_DB_NAME}
      table: public.memory_vector_store
      dimension: 1024
      create-table: true
      drop-table-first: false
```

说明：表结构由向量存储组件自动创建；本文件中的 `memory_vector_store` DDL 仅为语义说明。

### 2) 数据访问层（记忆条目）
- 新增实体：`src/main/java/org/xhy/domain/memory/model/MemoryItemEntity.java`
  - `@TableName("memory_items")`，字段对齐本文件定义（`id,userId,type,text,data,importance,tags,sourceSessionId,dedupeHash,status,createdAt,updatedAt`）。
- 新增仓储：`src/main/java/org/xhy/domain/memory/repository/MemoryItemRepository.java`
  - 继承 `MyBatisPlusExtRepository<MemoryItemEntity>`，提供 `selectByUserIdAndHash(...)` 等便捷方法。

### 3) 领域服务（存取与召回）
- 新增：`src/main/java/org/xhy/domain/memory/service/MemoryDomainService.java`
  - 依赖：`MemoryItemRepository`、`@Qualifier("memoryEmbeddingStore") EmbeddingStore<TextSegment>`、`EmbeddingModelFactory`。
  - 方法：
    - `List<String> saveMemories(String userId, String sessionId, List<CandidateMemory> candidates)`
      - 去重：按 `(userId, dedupeHash)`；新增或合并更新。
      - 写入 `memory_items` 后，同步写入向量存储（`text -> embedding`，`metadata` 冗余 `user_id,type,tags,item_id`）。
    - `List<MemoryResult> searchRelevant(String userId, String query, int topK)`
      - 使用 `memoryEmbeddingStore` 进行相似度检索；按 `importance` 与相似度加权排序；过滤 `status!=1`。

数据结构建议：
```
class CandidateMemory { String type; String text; Float importance; List<String> tags; }
class MemoryResult { String itemId; String type; String text; Float importance; Double score; }
```

### 4) 抽取服务（对话后写入）
- 新增：`src/main/java/org/xhy/domain/memory/service/MemoryExtractorService.java`
  - 依赖：`LLMServiceFactory` + 用户默认模型（与 `ConversationAppService` 一致的选择策略）。
  - 方法：`List<CandidateMemory> extract(String userId, String sessionId, String userMsg, String aiMsg)`
  - 产出结构即 `CandidateMemory` 列表；空则返回 `[]`。

### 5) 对话流程的接入点
- 读取注入（对话前）：
  - 位置：`src/main/java/org/xhy/application/conversation/service/message/AbstractMessageHandler.java:477` 的 `buildHistoryMessage(...)`。
  - 动作：在拼接系统提示词之前，调用 `memoryDomainService.searchRelevant(userId, userMessage, topK)`，将结果压缩为 3–6 条要点，合并到系统提示词中，或作为单独 `SystemMessage`（优先前者，减少消息数）。

- 写入抽取（对话后）：
  - 位置：`AbstractMessageHandler.onChatCompleted(...)`（同文件 172 行定义，已在流程成功/失败分支调用）。
  - 动作：异步执行 `extract(...)` → `saveMemories(...)`，避免拉高对话延迟；失败不影响主流程。

### 6) 元数据与常量
- 新增常量接口：`src/main/java/org/xhy/domain/memory/constant/MemoryMetadataConstant.java`
  - `USER_ID = "USER_ID"`、`ITEM_ID = "ITEM_ID"`、`MEMORY_TYPE = "MEMORY_TYPE"`、`TAGS = "TAGS"`。
  - 与 RAG 的 `MetadataConstant` 平行，避免混用。

### 7) 一致性与维度
- 维度需与现有嵌入模型一致（`application.yml: embedding.vector-store.dimension`）；内外两个向量表应保持相同维度，便于共用同一嵌入模型工厂。

### 8) 权限与可见性
- 读取/写入均以 `user_id` 为严格过滤条件；在 `metadata` 中冗余 `user_id`，使仅向量层检索也能过滤。

# Token超限处理功能实现任务清单

## 1. 数据库设计
- [x] 创建数据库扩展脚本 `token_overflow_strategy.sql`
  - [x] 在 context 表添加字段：
    - overflow_strategy (枚举类型：NONE, SLIDING_WINDOW, SUMMARIZE)
    - strategy_config (JSON格式)
    - summary (TEXT类型)
  - [x] 在 agents 和 agent_versions 表添加字段：
    - token_overflow_config (JSON格式)

## 2. 领域层设计

### 2.1 模型设计
- [x] 创建策略枚举类 `TokenOverflowStrategyEnum`
  - NONE (无策略)
  - SLIDING_WINDOW (滑动窗口)
  - SUMMARIZE (摘要策略)
- [x] 创建领域模型
  - [x] `TokenMessage` 消息模型 - 简化，只包含必要信息
  - [x] `TokenResult` 结果模型 - 统一返回结果
  - [x] `TokenOverflowConfig` 配置类 - 包含所有策略的参数
  - [x] `SlidingWindowConfig` 滑动窗口配置 (已合并到TokenOverflowConfig)
    - maxTokens：最大Token数
    - reserveRatio：预留缓冲比例
  - [x] `SummarizeConfig` 摘要策略配置 (已合并到TokenOverflowConfig)
    - summaryThreshold：触发摘要阈值
    - maxTokens：最大Token数

### 2.2 策略接口与实现
- [x] 创建策略接口 `TokenOverflowStrategy`
  - [x] `process(List<TokenMessage> messages)` 处理方法
  - [x] `getSummary()` 获取摘要方法
- [x] 实现三种策略类：
  - [x] `NoTokenOverflowStrategy` 无策略实现
  - [x] `SlidingWindowTokenOverflowStrategy` 滑动窗口实现
  - [x] `SummarizeTokenOverflowStrategy` 摘要策略实现
- [x] 创建策略工厂类 `TokenOverflowStrategyFactory`

### 2.3 领域服务
- [x] 创建 `TokenDomainService` 领域服务
  - [x] 封装策略处理核心逻辑
  - [x] 提供Token计算方法
  - [x] 提供统一的`processMessages`方法返回TokenResult

## 3. 应用层设计
- [ ] 创建 `TokenOverflowService` 应用服务
  - [ ] `processMessages` 处理消息并应用策略
  - [ ] `updateStrategyConfig` 更新策略配置
  - [ ] `getStrategyConfig` 获取策略配置
- [ ] 创建数据传输对象：
  - [ ] `TokenOverflowConfigDTO` 配置DTO
  - [ ] `TokenOverflowResultDTO` 结果DTO
- [ ] 创建 `TokenOverflowAssembler` 转换器
  - [ ] DTO与领域对象之间的转换

## 4. 接口层设计
- [ ] 扩展 `AgentController` 增加Token超限配置相关接口
  - [ ] `updateTokenOverflowConfig` 更新Token策略配置
  - [ ] `getTokenOverflowConfig` 获取Token策略配置
- [ ] 创建请求/响应类：
  - [ ] `TokenOverflowConfigRequest`
  - [ ] `TokenOverflowConfigResponse`
- [ ] 在会话消息处理流程中集成Token超限处理

## 5. 基础设施层设计

### 5.1 仓储实现
- [ ] 创建 `TokenOverflowRepositoryImpl` 实现类
  - [ ] 提供获取/更新Context摘要和活跃消息的方法
  - [ ] 提供获取/更新Agent Token配置的方法

### 5.2 工具类
- [ ] 创建 `TokenCalculator` 工具类
  - [ ] 实现消息Token计算方法
  - [ ] 提供缓存机制优化性能
- [ ] 创建 `SummaryGenerator` 工具类
  - [ ] 实现基于LLM的摘要生成
  - [ ] 处理异步摘要生成逻辑

### 5.3 持久化对象与Mapper
- [ ] 扩展持久化对象：
  - [ ] 扩展 `ContextPO`
  - [ ] 扩展 `AgentPO`
  - [ ] 扩展 `AgentVersionPO`
- [ ] 更新相应Mapper：
  - [ ] 更新 `ContextMapper`
  - [ ] 更新 `AgentMapper`
  - [ ] 更新 `AgentVersionMapper`

## 6. 集成与测试
- [ ] 创建单元测试：
  - [ ] `TokenOverflowStrategyTest` 测试各策略实现
  - [ ] `TokenCalculatorTest` 测试Token计算
  - [ ] `SummaryGeneratorTest` 测试摘要生成
- [ ] 创建集成测试：
  - [ ] 测试滑动窗口场景
  - [ ] 测试摘要生成场景
  - [ ] 测试策略切换场景

## 7. 文档与部署
- [ ] 更新部署文档
  - [ ] 添加新的数据库迁移说明
  - [ ] 添加配置参数说明
- [ ] 编写功能使用文档
  - [ ] 各策略的使用场景和建议
  - [ ] 配置参数的最佳实践 
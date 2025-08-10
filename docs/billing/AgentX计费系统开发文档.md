# AgentX 计费系统开发文档

## 1. 项目概述

### 1.1 系统目标
为AgentX平台构建一个高度抽象、可扩展的计费系统，支持多种业务场景的灵活计费需求。

### 1.2 核心设计原则
- **关注点分离**：商品定义、计费规则、计算策略完全解耦
- **DDD架构**：严格遵循项目DDD分层规范
- **策略模式**：通过策略模式支持多样化计费算法
- **业务无侵入**：核心业务逻辑保持干净，通过配置驱动计费

### 1.3 支持的计费场景
- 🤖 **模型调用计费**：按Token输入输出分别计费 ✅ **已完成集成**
- 🎯 **Agent创建计费**：按次数固定收费 ✅ **已预留配置，待业务集成**
- 📊 **Agent使用计费**：按调用次数计费 ⏳ **待开发**
- 🔌 **API调用计费**：按接口类型分层计费 ⏳ **待开发**
- 💾 **存储使用计费**：按存储量阶梯计费 ⏳ **待开发**

## 2. 技术架构设计

### 2.1 架构概览
```
┌──────────────────────────────────────────────────┐
│                Interface Layer                   │
│    BillingController / AccountController         │
└────────────────┬─────────────────────────────────┘
                 │
┌────────────────▼─────────────────────────────────┐
│              Application Layer                   │
│  BillingAppService / AccountAppService          │
│  DTO / Assembler / Exception Handler            │
└────────────────┬─────────────────────────────────┘
                 │
┌────────────────▼─────────────────────────────────┐
│               Domain Layer                       │
│  Entity / Repository Interface / Domain Service │
└────────────────┬─────────────────────────────────┘
                 │
┌────────────────▼─────────────────────────────────┐
│            Infrastructure Layer                  │
│  Repository Impl / Strategy Factory / Converter │
└──────────────────────────────────────────────────┘
```

### 2.2 核心工作流程
```
1. 业务触发 → 2. 查找商品 → 3. 获取规则 → 4. 选择策略
     ↓             ↓            ↓            ↓
8. 记录用量 ← 7. 更新余额 ← 6. 执行扣费 ← 5. 计算费用
```

#### 2.2.1 商品查找机制
计费系统通过**业务主键组合**查找对应商品：
- **查询条件**：`type` + `service_id` 的组合作为业务主键
- **查询逻辑**：`SELECT * FROM products WHERE type = ? AND service_id = ?`
- **示例说明**：
  - 模型调用：`type='MODEL_USAGE'` + `service_id='123'` (模型表主键ID)
  - Agent创建：`type='AGENT_CREATION'` + `service_id='agent_creation'` (固定标识)
  - 具体Agent使用：`type='AGENT_USAGE'` + `service_id='789'` (Agent表主键ID)

### 2.3 策略模式设计
- **策略接口**：`BillingStrategy`定义计费算法契约
- **策略工厂**：`BillingStrategyFactory`管理策略实例
- **策略实现**：`ModelTokenStrategy`、`PerUnitStrategy`等具体算法

### 2.4 数据模型关系
```
Products (商品) → Rules (规则) → Strategy (策略实现)
    ↓               ↓              ↓
pricing_config   handler_key   具体算法类
```

## 3. 数据库设计

### 3.1 表结构概览

#### 3.1.1 核心业务表
- **products**: 商品配置表，定义计费项和价格
- **rules**: 计费规则表，定义抽象算法类型  
- **accounts**: 用户账户表，管理余额和信用额度
- **usage_records**: 用量记录表，审计和账单生成

#### 3.1.2 关键字段设计
- **service_id语义**：业务表主键ID，配合type字段唯一定位计费商品
  - `MODEL_USAGE`: 模型表的主键ID (如`123`、`456`)，对应models表的id字段
  - `AGENT_CREATION`: 固定业务标识符 (`agent_creation`)，因为不对应具体记录
  - `AGENT_USAGE`: Agent表的主键ID (如`789`、`101`)，对应agents表的id字段
  - `API_CALL`: API类型的主键ID，对应相应配置表的id字段
- **业务主键组合**：`(type, service_id)` 构成唯一业务主键
- **数据关联方式**：通过业务表主键ID间接关联，避免直接外键依赖
- **pricing_config**: JSON格式存储灵活价格配置
- **quantity_data**: JSON格式记录具体用量信息

#### 3.1.3 商品查询设计原则
- **解耦设计**：计费系统不直接依赖业务表，通过service_id间接关联
- **灵活映射**：同一类型业务可配置多个不同的计费商品
- **查询效率**：`(type, service_id)` 建立联合唯一索引保证查询性能

### 3.2 数据库迁移策略
- **V20250726001**: 创建基础表结构
- **V20250726002**: 初始化基础数据(规则、常用模型配置)
- **索引优化**: 针对查询热点建立复合索引

## 4. 开发规范

### 4.1 DDD分层规范

#### 4.1.1 包结构设计
```
org.xhy.domain.billing.*      // 领域层
org.xhy.application.billing.* // 应用层  
org.xhy.infrastructure.billing.* // 基础设施层
org.xhy.interfaces.billing.*  // 接口层
```

#### 4.1.2 依赖关系约束
- ✅ **Infrastructure → Domain** 
- ✅ **Application → Infrastructure + Domain**
- ❌ **Infrastructure → Application** (严格禁止)
- ❌ **Domain → Infrastructure** (通过接口反转)

### 4.2 编码规范

#### 4.2.1 实体设计规范
- 继承`BaseEntity`获得审计字段
- 实现业务验证方法(如`isValid()`)
- 封装业务行为(如`AccountEntity.deduct()`)
- 使用`BigDecimal`确保金额精度

#### 4.2.2 Repository规范
- 接口定义在Domain层
- 实现类在Infrastructure层
- 继承`MyBatisPlusExtRepository`获得权限检查
- 命名约定：`findXxx()`、`getXxx()`、`existsXxx()`

#### 4.2.3 Service规范
- **Domain Service**: 纯业务逻辑，无事务
- **Application Service**: 流程编排，事务边界
- 使用构造函数注入确保依赖明确
- 异常处理统一使用`BusinessException`

### 4.3 安全规范

#### 4.3.1 并发控制
- 账户余额更新使用悲观锁(`selectForUpdate`)
- 关键业务操作添加`@Transactional`
- 幂等性通过`request_id`确保

#### 4.3.2 数据校验
- API层：格式校验(`@Validated`)
- Application层：业务规则校验
- Domain层：实体状态校验

## 5. 实施计划

### 5.1 开发阶段

#### 阶段一：基础架构 (Week 1-2)
- [ ] 数据库表结构创建
- [ ] Domain层实体和仓储接口
- [ ] 基础设施层Repository实现
- [ ] 策略模式框架搭建

#### 阶段二：核心功能 (Week 3-4)  
- [ ] 计费策略实现(Token计费、按次计费)
- [ ] Application层服务实现
- [ ] 基础API接口开发
- [ ] 单元测试编写

#### 阶段三：业务集成 (Week 5-6)
- [x] 模型调用计费集成
- [ ] Agent创建计费集成 **（已预留配置，待集成到AgentAppService）**
- [ ] 前端账户管理页面
- [ ] 集成测试

#### 阶段四：扩展优化 (Week 7-8)
- [ ] 高级计费策略(分层、订阅)
- [ ] 支付集成
- [ ] 报表统计
- [ ] 性能优化

### 5.2 关键里程碑
- **M1**: 基础计费功能可用
- **M2**: 模型计费完整集成
- **M3**: 前端用户界面完成
- **M4**: 生产环境部署就绪

## 6. 集成点设计

### 6.1 模型调用计费集成
- **集成位置**：`AbstractMessageHandler.onCompleteResponse`
- **计费时机**：模型响应完成后
- **商品查找**：通过`type='MODEL_USAGE'` + `service_id=modelPrimaryKey`查找对应计费商品
- **数据来源**：输入Token数、输出Token数、模型主键ID
- **计费逻辑**：
  ```
  BillingContext context = BillingContext.builder()
      .type("MODEL_USAGE")
      .serviceId(chatContext.getModel().getId().toString())  // 使用模型表主键ID
      .usageData(Map.of("input", inputTokens, "output", outputTokens))
      .requestId(generateRequestId())
      .build();
  billingService.charge(userId, context);
  ```
- **异常处理**：余额不足时优雅降级

### 6.2 Agent创建计费集成 ✅ **已预留配置，待业务集成**
- **预留状态**：计费规则和商品配置已完整预留，框架完全支持
- **集成位置**：`AgentAppService.createAgent`
- **计费时机**：创建前预检查，创建后计费
- **商品查找**：通过`type='AGENT_CREATION'` + `service_id='agent_creation'`查找固定计费商品
- **数据来源**：固定单价配置（此场景下不依赖具体业务记录）
- **已配置价格**：每创建一个Agent收费 **10.0元**（可在products表中调整）
- **计费逻辑**：
  ```java
  // TODO: 在AgentAppService.createAgent中集成以下逻辑
  BillingContext context = BillingContext.builder()
      .type(BillingType.AGENT_CREATION.getCode())
      .serviceId("agent_creation")  // 固定业务标识，不是主键ID
      .usageData(Map.of("quantity", 1))
      .requestId(generateRequestId())
      .build();
  billingService.charge(userId, context);
  ```
- **异常处理**：余额不足阻止创建
- **数据库配置**：
  - 商品ID: `product-agent-creation`
  - 规则ID: `rule-per-unit` 
  - 策略类型: `PER_UNIT_STRATEGY`
  - 定价配置: `{"cost_per_unit": 10.0}`

### 6.3 Agent使用计费集成
- **集成位置**：Agent调用处理逻辑
- **计费时机**：Agent调用完成后
- **商品查找**：通过`type='AGENT_USAGE'` + `service_id=agentPrimaryKey`查找对应计费商品
- **数据来源**：Agent主键ID、调用次数
- **计费逻辑**：
  ```
  BillingContext context = BillingContext.builder()
      .type("AGENT_USAGE")
      .serviceId(agentEntity.getId().toString())  // 使用Agent表主键ID
      .usageData(Map.of("calls", 1))
      .requestId(generateRequestId())
      .build();
  billingService.charge(userId, context);
  ```
- **异常处理**：余额不足时限制调用

### 6.4 API调用计费集成
- **集成位置**：Gateway层或拦截器
- **计费时机**：请求处理完成后
- **商品查找**：通过`type='API_CALL'` + `service_id=apiConfigId`查找对应计费商品
- **数据来源**：API配置主键ID和调用次数
- **异常处理**：配置限流和熔断

## 7. 前端设计

### 7.1 页面结构
- **账户概览页**：余额、消费统计、充值入口
- **用量明细页**：分页查询、筛选、导出
- **计费配置页**：管理员商品和规则管理

### 7.2 用户体验设计
- **余额预警**：低余额Toast提醒
- **实时更新**：余额变动即时刷新
- **透明计费**：详细展示计费明细和规则

## 8. 测试策略

### 8.1 测试层次
- **单元测试**：Domain层业务逻辑测试
- **集成测试**：Repository和数据库交互测试  
- **接口测试**：API功能和异常情况测试
- **端到端测试**：完整业务流程测试

### 8.2 测试重点
- **并发安全**：多用户同时扣费测试
- **幂等性**：重复请求处理测试
- **精度验证**：金额计算精度测试
- **异常恢复**：各种异常场景的处理测试

## 9. 运维监控

### 9.1 关键指标监控
- **计费成功率**：正常计费vs异常计费比例
- **余额预警**：低余额用户数量监控
- **性能指标**：计费接口响应时间
- **数据一致性**：账户余额与用量记录对账

### 9.2 告警机制
- **系统异常**：计费服务不可用告警
- **业务异常**：大量余额不足告警  
- **数据异常**：对账不一致告警

## 10. 部署指南

### 10.1 环境配置
- **数据库**：PostgreSQL 13+，确保DECIMAL精度支持
- **缓存**：Redis用于策略实例缓存
- **监控**：集成OTel进行链路追踪

### 10.2 部署检查清单
- [ ] 数据库迁移脚本执行
- [ ] 基础数据初始化(规则、商品配置)
- [ ] 配置文件安全检查(无敏感信息)
- [ ] 监控和告警配置验证
- [ ] 备份恢复流程测试

## 11. 风险控制

### 11.1 技术风险
- **并发问题**：通过数据库锁和事务确保一致性
- **精度丢失**：统一使用BigDecimal处理金额
- **性能瓶颈**：建立索引和缓存策略

### 11.2 业务风险  
- **计费错误**：完善的测试和审计机制
- **恶意刷量**：接口限流和异常检测
- **数据泄露**：敏感数据加密和访问控制

## 12. 后续扩展

### 12.1 高级特性
- **订阅套餐**：包月包年计费模式
- **优惠券系统**：折扣和促销活动支持
- **分销体系**：多级代理和佣金计算
- **多币种支持**：国际化计费需求

### 12.2 技术演进
- **实时计费**：流式处理大规模计费场景
- **智能定价**：基于用量模式的动态定价
- **预测分析**：用户消费趋势预测和预警
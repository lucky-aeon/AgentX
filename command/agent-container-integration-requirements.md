# Agent平台与容器管理业务集成需求文档

## 项目背景

AgentX平台已完成基础的容器管理功能，现需要将容器管理与Agent对话流程进行深度集成，实现用户工具的自动化部署和管理。

## 已完成功能

### 1. 容器管理基础设施
- ✅ 容器模板管理：管理员可创建和管理容器模板
- ✅ 用户容器CRUD：创建、删除、启动、停止用户容器  
- ✅ 容器自动清理：1天不使用暂停，5天不使用销毁
- ✅ 容器状态管理：包括CREATING、RUNNING、STOPPED、ERROR、DELETING、DELETED、SUSPENDED
- ✅ 端口管理：自动分配外部端口，避免冲突
- ✅ 数据卷挂载：为每个用户容器创建独立数据目录

### 2. 工具管理基础
- ✅ 工具全局状态管理：管理员可设置工具是否为全局状态
- ✅ 工具审核流程：审核容器已在系统中默认配置，无需额外开发

## 核心业务集成需求

### 1. Agent对话流程集成

根据业务流程图，需要实现以下完整流程：

#### 1.1 对话启动阶段
```
开始 → 对话 → Agent是否有工具？
```
- **决策点**：检查当前Agent是否配置了工具
- **是**：继续后续流程
- **否**：直接结束对话

#### 1.2 用户工具查询阶段  
```
查询用户已安装工具 → 查询工具集状态
```
- **功能**：获取当前用户已安装的工具列表
- **功能**：检查工具的全局/非全局状态
- **说明**：暂时不实现全局工具概念，所有工具都按非全局处理

#### 1.3 容器管理阶段
```
是否注册容器？ → 创建用户容器 / 查询健康状态 → 注册容器
```
- **检查用户容器**：判断当前用户是否已有容器
- **容器创建**：如果没有容器，则创建新的用户容器
- **健康检查**：如果有容器，检查容器健康状态
- **容器恢复**：如果容器被暂停，自动恢复运行状态
- **重新注册**：确保容器可用后注册到系统

#### 1.4 工具部署阶段
```
是否部署到用户容器？ → 部署到用户容器 → 发起对话
```
- **部署检查**：检查工具是否已部署到用户容器的MCP网关
- **自动部署**：将未部署的工具部署到用户容器
- **对话启动**：完成部署后启动Agent对话

### 2. MCP网关开发

#### 2.1 网关架构
- **部署方式**：每个用户容器内运行独立的MCP网关
- **端口配置**：容器内固定8080端口，映射到宿主机随机端口
- **镜像管理**：使用统一的MCP网关Docker镜像

#### 2.2 核心接口

##### 用户已安装工具查询
```
GET /mcp/tools/installed?userId={userId}
Response: {
  "code": 200,
  "data": [
    {
      "toolId": "string",
      "toolName": "string", 
      "version": "string",
      "status": "deployed|pending|error"
    }
  ]
}
```

##### 工具集状态查询
```
GET /mcp/tools/status?userId={userId}&toolIds={toolIds}
Response: {
  "code": 200,
  "data": {
    "global": false,  // 暂时固定为false
    "tools": [
      {
        "toolId": "string",
        "deployed": true|false,
        "status": "deployed|pending|error"
      }
    ]
  }
}
```

##### 工具部署接口
```
POST /mcp/tools/deploy
Request: {
  "userId": "string",
  "tools": [
    {
      "toolId": "string",
      "toolName": "string",
      "version": "string",
      "config": {}  // 工具配置信息
    }
  ]
}
Response: {
  "code": 200,
  "message": "部署成功",
  "data": {
    "deployedTools": ["toolId1", "toolId2"],
    "failedTools": []
  }
}
```

##### MCP网关健康检查
```
GET /mcp/health
Response: {
  "code": 200,
  "data": {
    "status": "healthy",
    "toolsCount": 5,
    "uptime": "2h30m"
  }
}
```

### 3. 容器管理模块扩展

#### 3.1 业务接口

##### 用户容器检查
```
GET /api/containers/user/{userId}/exists
Response: {
  "code": 200,
  "data": {
    "exists": true|false,
    "containerId": "string",
    "status": "RUNNING|STOPPED|SUSPENDED"
  }
}
```

##### 用户容器创建
```
POST /api/containers/user
Request: {
  "userId": "string",
  "containerName": "string"  // 可选，默认生成
}
Response: {
  "code": 200,
  "data": {
    "containerId": "string",
    "status": "CREATING",
    "externalPort": 30001,
    "mcpGatewayUrl": "http://localhost:30001"
  }
}
```

##### 容器健康状态查询
```
GET /api/containers/{containerId}/health
Response: {
  "code": 200,
  "data": {
    "containerId": "string",
    "status": "RUNNING",
    "healthy": true,
    "mcpGatewayUrl": "http://localhost:30001",
    "lastAccessedAt": "2025-06-28T10:00:00"
  }
}
```

##### 容器恢复
```
POST /api/containers/{containerId}/resume
Response: {
  "code": 200,
  "data": {
    "containerId": "string", 
    "status": "RUNNING",
    "mcpGatewayUrl": "http://localhost:30001"
  }
}
```

#### 3.2 集成逻辑

##### 容器创建标准流程
1. **镜像拉取**：使用配置的MCP网关镜像
2. **端口分配**：分配30000-40000范围内未占用端口
3. **数据卷挂载**：挂载`/docker/users/{userId}`到容器
4. **环境变量**：设置必要的环境变量
5. **网络配置**：加入默认Docker网络
6. **启动检查**：确保容器成功启动并可访问

##### 容器健康检查机制
1. **Docker状态检查**：检查容器是否正在运行
2. **网络连通性**：检查端口是否可访问
3. **MCP网关响应**：调用MCP网关健康检查接口
4. **最后访问时间更新**：更新容器访问时间

### 4. Agent对话服务集成

#### 4.1 核心服务类

##### ContainerIntegrationAppService
负责整个容器集成流程的编排：

```java
@Service
public class ContainerIntegrationAppService {
    
    /**
     * 确保用户容器可用并部署所需工具
     */
    public ContainerReadyResult ensureUserContainerReady(String userId, List<String> requiredToolIds) {
        // 1. 检查用户是否有容器
        // 2. 创建容器（如果不存在）
        // 3. 检查容器健康状态
        // 4. 恢复容器（如果被暂停）
        // 5. 检查工具部署状态
        // 6. 部署未安装的工具
        // 7. 返回容器就绪结果
    }
    
    /**
     * 获取用户已安装工具列表
     */
    public List<UserInstalledTool> getUserInstalledTools(String userId) {
        // 调用MCP网关接口获取已安装工具
    }
    
    /**
     * 检查工具集状态
     */
    public ToolSetStatus checkToolSetStatus(String userId, List<String> toolIds) {
        // 调用MCP网关接口检查工具状态
    }
}
```

#### 4.2 Agent对话流程修改

修改`AgentConversationFlowService`，在对话开始前调用容器集成服务：

```java
public ChatResponse startConversation(ChatRequest request) {
    // 1. 检查Agent是否有工具
    List<String> agentToolIds = getAgentToolIds(request.getAgentId());
    if (agentToolIds.isEmpty()) {
        return endConversation("Agent没有配置工具");
    }
    
    // 2. 确保用户容器就绪
    String userId = UserContext.getCurrentUserId();
    ContainerReadyResult containerResult = containerIntegrationAppService
        .ensureUserContainerReady(userId, agentToolIds);
    
    if (!containerResult.isReady()) {
        return errorResponse("容器准备失败: " + containerResult.getErrorMessage());
    }
    
    // 3. 继续原有对话流程
    return continueConversation(request, containerResult.getMcpGatewayUrl());
}
```

### 5. 错误处理和重试机制

#### 5.1 异常类型
- **容器创建失败**：Docker服务异常、资源不足
- **端口分配失败**：端口耗尽、网络配置错误  
- **MCP网关不可达**：网络问题、服务未启动
- **工具部署失败**：工具配置错误、依赖缺失

#### 5.2 重试策略
- **容器创建**：失败后重试1次，仍失败则报错
- **工具部署**：失败后重试2次，记录失败工具列表
- **健康检查**：失败后等待5秒重试，最多3次

#### 5.3 用户反馈
- **创建中状态**：显示"正在准备工作环境..."
- **部署中状态**：显示"正在安装工具..."
- **失败状态**：显示具体错误信息和建议操作

### 6. 数据持久化

#### 6.1 容器使用记录
- **访问时间更新**：每次对话时更新`last_accessed_at`
- **使用统计**：记录容器使用频率和时长
- **状态同步**：确保数据库状态与Docker实际状态一致

#### 6.2 工具部署记录
- **部署状态跟踪**：记录哪些工具已部署到哪些容器
- **版本管理**：支持工具版本更新和回滚
- **配置持久化**：保存工具配置信息

### 7. 性能优化

#### 7.1 缓存策略
- **容器状态缓存**：缓存容器健康状态，减少检查频率
- **工具列表缓存**：缓存用户已安装工具列表
- **MCP网关连接池**：复用HTTP连接，提高响应速度

#### 7.2 异步处理
- **容器创建异步化**：创建容器时返回任务ID，异步查询进度
- **工具部署批量化**：批量部署多个工具，提高效率
- **健康检查定时化**：定时检查容器健康状态，而非实时检查

### 8. 监控和日志

#### 8.1 关键指标
- **容器创建成功率**：监控容器创建的成功率
- **工具部署成功率**：监控工具部署的成功率  
- **对话启动延迟**：监控从请求到对话开始的时间
- **容器资源使用率**：监控CPU、内存使用情况

#### 8.2 日志记录
- **操作日志**：记录容器创建、删除、启动、停止等操作
- **错误日志**：记录详细的错误信息和堆栈
- **性能日志**：记录关键操作的耗时
- **用户行为日志**：记录用户的容器使用行为

## 实施计划

### Phase 1: 核心接口开发（预计3-5天）
1. 开发MCP网关基础服务和API接口
2. 完善容器管理模块的业务接口
3. 实现容器创建和健康检查逻辑

### Phase 2: 业务流程集成（预计2-3天）
1. 开发ContainerIntegrationAppService
2. 修改Agent对话流程集成容器管理
3. 实现错误处理和重试机制

### Phase 3: 测试和优化（预计2-3天）
1. 端到端流程测试
2. 性能优化和缓存策略实施
3. 监控和日志完善

### Phase 4: 部署和验证（预计1-2天）
1. 生产环境部署
2. 功能验证和bug修复
3. 文档更新

## 技术要点

### 1. 容器镜像设计
- **基础镜像**：基于官方Node.js或Python镜像
- **MCP网关**：内置MCP协议处理逻辑
- **工具管理**：支持动态安装和卸载工具
- **配置管理**：支持环境变量和配置文件

### 2. 网络架构
- **容器网络**：使用Docker默认bridge网络
- **端口映射**：宿主机端口映射到容器8080端口
- **防火墙**：确保端口访问安全性
- **负载均衡**：为全局工具预留负载均衡接口

### 3. 安全考虑
- **容器隔离**：确保用户容器之间数据隔离
- **权限控制**：限制容器对宿主机的访问权限
- **网络安全**：限制容器网络访问范围
- **数据加密**：敏感配置信息加密存储

## 验收标准

### 1. 功能验收
- ✅ 用户可以正常发起Agent对话
- ✅ 系统自动为用户创建和管理容器
- ✅ 工具能够正确部署到用户容器
- ✅ 容器自动清理机制正常工作
- ✅ 异常情况有合适的错误提示

### 2. 性能验收
- ✅ 容器创建时间 < 30秒
- ✅ 工具部署时间 < 10秒
- ✅ 对话启动延迟 < 5秒
- ✅ 系统支持至少50个并发用户

### 3. 稳定性验收
- ✅ 容器创建成功率 > 95%
- ✅ 工具部署成功率 > 98%
- ✅ 系统7*24小时稳定运行
- ✅ 自动恢复机制正常工作

## 风险评估

### 1. 技术风险
- **Docker服务稳定性**：Docker服务异常可能影响容器管理
- **端口资源耗尽**：大量用户可能导致端口不足
- **MCP协议兼容性**：新版本工具可能存在协议兼容问题

### 2. 业务风险  
- **用户体验**：容器创建时间过长影响用户体验
- **资源消耗**：大量容器可能消耗过多系统资源
- **数据安全**：用户数据隔离不当可能造成安全问题

### 3. 运维风险
- **监控盲区**：容器状态监控不及时可能影响服务质量
- **故障排查**：分布式架构增加故障排查难度
- **版本升级**：MCP网关版本升级可能影响现有容器

## 总结

本需求文档详细描述了Agent平台与容器管理的业务集成方案。通过完善的容器管理、MCP网关开发和业务流程集成，将实现用户工具的自动化部署和管理，为用户提供便捷、稳定的Agent对话服务。

**注意**：审核容器已在系统中默认配置，无需额外开发。全局工具概念暂不实现，后续根据业务需要再扩展。
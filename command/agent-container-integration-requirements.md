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

---

# 详细技术实现方案

## 基于现有代码的深度集成分析

### 当前架构分析

通过对现有代码的深入分析，发现系统已经具备了较为完整的基础架构：

1. **Agent对话流程服务**：`AgentConversationFlowService.startAgentConversationFlow()` 已实现流程图的核心逻辑
2. **容器集成服务**：`ContainerIntegrationAppService` 提供了容器操作的具体实现
3. **工具管理器**：`AgentToolManager.createToolProvider()` 负责创建MCP工具提供者
4. **MCP网关服务**：`MCPGatewayService.getSSEUrl()` 提供SSE连接URL

### 核心问题识别

**关键问题**：当前 `MCPGatewayService.getSSEUrl()` 方法固定使用yml配置的全局URL，无法根据工具类型和用户隔离需求动态选择连接地址。

```java
// 当前实现 - 存在的问题
public String getSSEUrl(String mcpServerName) {
    // 固定使用全局地址，无法支持用户隔离工具
    return properties.getBaseUrl() + "/" + mcpServerName + "/sse/sse?api_key=" + properties.getApiKey();
}
```

**业务逻辑要求**：
- **全局工具**：继续使用yml配置的全局MCP Gateway URL
- **用户隔离工具**：动态获取用户容器IP+Port，组装专属SSE URL
- **容器依赖**：依赖现有容器，不自动创建（需要用户预先创建容器）
- **架构原则**：遵循DDD分层架构，基础设施层依赖领域层

**核心实现策略**：
在创建MCP客户端时，根据工具的isGlobal字段判断连接策略，简单路由到相应的Gateway URL。

## 详细实现方案

### 1. MCPGatewayService 核心改造

#### 1.1 简化的URL路由机制

实际实现：遵循DDD架构原则，基础设施层依赖领域层，实现简洁的URL路由。

```java
@Service
public class MCPGatewayService {
    
    private final MCPGatewayProperties properties;
    private final ContainerDomainService containerDomainService;  // 领域层依赖 ✅
    private final ToolDomainService toolDomainService;           // 领域层依赖 ✅
    
    public MCPGatewayService(MCPGatewayProperties properties, 
                           ContainerDomainService containerDomainService,
                           ToolDomainService toolDomainService) {
        this.properties = properties;
        this.containerDomainService = containerDomainService;
        this.toolDomainService = toolDomainService;
    }

    /**
     * 智能获取SSE URL：根据工具类型选择连接策略
     * 
     * @param mcpServerName 工具服务名称
     * @param userId 用户ID（用户工具必需）
     * @return 对应的SSE连接URL
     */
    public String getSSEUrl(String mcpServerName, String userId) {
        // 1. 判断工具类型
        boolean isGlobalTool = isGlobalTool(mcpServerName);
        
        if (isGlobalTool) {
            // 全局工具：使用yml配置的全局Gateway
            return buildGlobalSSEUrl(mcpServerName);
        } else {
            // 用户隔离工具：使用用户容器Gateway
            if (userId == null || userId.trim().isEmpty()) {
                throw new BusinessException("用户隔离工具需要提供用户ID: " + mcpServerName);
            }
            return buildUserContainerSSEUrl(mcpServerName, userId);
        }
    }
    
    /**
     * 判断是否为全局工具
     */
    private boolean isGlobalTool(String mcpServerName) {
        try {
            ToolEntity tool = toolDomainService.getToolByServerName(mcpServerName);
            return tool != null && tool.isGlobal();
        } catch (Exception e) {
            logger.warn("无法判断工具类型，默认为全局工具: {}", mcpServerName, e);
            return true; // 默认为全局工具，确保向后兼容
        }
    }
    
    /**
     * 构建全局工具SSE URL
     */
    private String buildGlobalSSEUrl(String mcpServerName) {
        return properties.getBaseUrl() + "/" + mcpServerName + "/sse/sse?api_key=" + properties.getApiKey();
    }
    
    /**
     * 构建用户容器工具SSE URL（简化实现）
     */
    private String buildUserContainerSSEUrl(String mcpServerName, String userId) {
        try {
            logger.info("获取用户容器信息: userId={}, tool={}", userId, mcpServerName);
            
            // 1. 查询用户容器（不自动创建）
            ContainerEntity containerInfo = getUserContainerEntity(userId);
            
            // 2. 验证容器状态
            if (!isContainerHealthy(containerInfo)) {
                throw new BusinessException("用户容器不可用：userId=" + userId + ", status=" + containerInfo.getStatus());
            }
            
            // 3. 构建容器SSE URL
            String containerBaseUrl = "http://" + containerInfo.getIpAddress() + ":" + containerInfo.getExternalPort();
            String sseUrl = containerBaseUrl + "/" + mcpServerName + "/sse/sse?api_key=" + properties.getApiKey();
            
            // 4. 工具健康检查（TODO: 待实现）
            validateToolInContainer(containerBaseUrl, mcpServerName);
            
            logger.info("用户容器工具连接就绪: userId={}, url={}", userId, maskSensitiveInfo(sseUrl));
            return sseUrl;
            
        } catch (Exception e) {
            logger.error("构建用户容器SSE URL失败: userId={}, tool={}", userId, mcpServerName, e);
            throw new BusinessException("无法连接用户工具：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户容器实体（仅查询，不创建）
     */
    private ContainerEntity getUserContainerEntity(String userId) {
        ContainerEntity userContainer = containerDomainService.getUserContainer(userId);
        
        if (userContainer == null) {
            throw new BusinessException("用户容器不存在，请先在管理后台创建容器: userId=" + userId);
        }
        
        return userContainer;
    }
    
    /**
     * 检查容器是否健康
     */
    private boolean isContainerHealthy(ContainerEntity container) {
        if (container == null) {
            return false;
        }
        
        // 检查容器状态是否为运行中
        boolean isRunning = ContainerStatus.RUNNING.equals(container.getStatus());
        
        // 检查必要的网络信息是否存在
        boolean hasNetworkInfo = container.getIpAddress() != null && 
                                container.getExternalPort() != null;
        
        return isRunning && hasNetworkInfo;
    }
    
    /**
     * 验证容器内工具状态（TODO: 待实现完整接口）
     */
    private void validateToolInContainer(String containerBaseUrl, String toolName) {
        try {
            String healthUrl = containerBaseUrl + "/mcp/tools/health?tool=" + toolName;
            // TODO: 实现HTTP调用，验证工具是否在容器中正常运行
            logger.debug("TODO: 验证容器内工具状态: url={}", healthUrl);
        } catch (Exception e) {
            logger.warn("容器内工具健康检查失败（当前忽略）: tool={}", toolName, e);
            // 当前暂不抛出异常，等待接口实现后再启用
        }
    }
    
    /**
     * 屏蔽敏感信息
     */
    private String maskSensitiveInfo(String url) {
        if (url == null) return null;
        return url.replaceAll("api_key=[^&]*", "api_key=***");
    }
    
    /**
     * 兼容性方法：保持向后兼容
     */
    public String getSSEUrl(String mcpServerName) {
        return getSSEUrl(mcpServerName, null, true);
    }
}
```

#### 1.2 容器信息查询接口

```java
@Service  
public class ContainerAppService {
    
    /**
     * 获取用户容器详细信息
     * 
     * @param userId 用户ID
     * @return 容器信息，如果容器不存在返回null
     */
    public ContainerInfo getUserContainerInfo(String userId) {
        try {
            // 1. 查询用户容器实体
            ContainerEntity container = containerDomainService.getUserContainer(userId);
            if (container == null) {
                logger.info("用户容器不存在: userId={}", userId);
                return null;
            }
            
            // 2. 检查Docker容器状态
            String dockerContainerId = container.getDockerContainerId();
            if (dockerContainerId == null) {
                logger.warn("用户容器Docker ID为空: userId={}, containerId={}", userId, container.getId());
                return ContainerInfo.unhealthy("Docker容器ID为空");
            }
            
            // 3. 检查Docker容器是否运行
            boolean isRunning = dockerService.isContainerRunning(dockerContainerId);
            if (!isRunning) {
                logger.info("Docker容器未运行: userId={}, dockerId={}", userId, dockerContainerId);
                return ContainerInfo.unhealthy("容器未运行");
            }
            
            // 4. 检查容器网络连接
            String ipAddress = container.getIpAddress();
            Integer port = container.getExternalPort();
            
            if (ipAddress == null || port == null) {
                logger.warn("容器网络信息不完整: userId={}, ip={}, port={}", userId, ipAddress, port);
                return ContainerInfo.unhealthy("网络信息不完整");
            }
            
            // 5. 检查MCP网关健康状态（可选）
            boolean mcpHealthy = checkMcpGatewayHealth(ipAddress, port);
            
            return new ContainerInfo(
                ipAddress, 
                port, 
                isRunning && mcpHealthy, 
                container.getStatus(),
                container.getId()
            );
            
        } catch (Exception e) {
            logger.error("获取用户容器信息失败: userId={}", userId, e);
            return ContainerInfo.unhealthy("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查MCP网关健康状态
     */
    private boolean checkMcpGatewayHealth(String ipAddress, Integer port) {
        try {
            String healthUrl = "http://" + ipAddress + ":" + port + "/mcp/health";
            // 发送HTTP健康检查请求
            // 这里可以使用HttpClient或RestTemplate实现
            return mcpGatewayService.checkHealth(healthUrl);
        } catch (Exception e) {
            logger.debug("MCP网关健康检查失败: {}:{}", ipAddress, port, e);
            return false; // 健康检查失败不影响容器基本可用性
        }
    }
    
    /**
     * 容器信息数据传输对象
     */
    public static class ContainerInfo {
        private final String ipAddress;
        private final Integer port;
        private final boolean healthy;
        private final ContainerStatus status;
        private final String containerId;
        private final String message;
        
        public ContainerInfo(String ipAddress, Integer port, boolean healthy, 
                           ContainerStatus status, String containerId) {
            this.ipAddress = ipAddress;
            this.port = port;
            this.healthy = healthy;
            this.status = status;
            this.containerId = containerId;
            this.message = healthy ? "容器健康" : "容器不健康";
        }
        
        private ContainerInfo(String message) {
            this.ipAddress = null;
            this.port = null;
            this.healthy = false;
            this.status = null;
            this.containerId = null;
            this.message = message;
        }
        
        public static ContainerInfo unhealthy(String reason) {
            return new ContainerInfo(reason);
        }
        
        public boolean isHealthy() {
            return healthy && status == ContainerStatus.RUNNING;
        }
        
        // getter方法...
    }
}
```

### 2. AgentToolManager 调用点简化

#### 2.1 核心改造：传递用户上下文

重点改造：在创建工具时传递用户ID，让MCPGatewayService自动处理URL路由。

```java
@Component
public class AgentToolManager {
    
    private final MCPGatewayService mcpGatewayService;
    
    /**
     * 创建工具提供者：支持全局/用户隔离工具自动识别
     * 
     * @param mcpServerNames 工具服务名列表
     * @param toolPresetParams 工具预设参数
     * @param userId 用户ID（关键参数：用于用户隔离工具）
     * @return 工具提供者实例
     */
    public ToolProvider createToolProvider(List<String> mcpServerNames, 
                                         Map<String, Map<String, Map<String, String>>> toolPresetParams,
                                         String userId) {
        if (mcpServerNames == null || mcpServerNames.isEmpty()) {
            logger.info("没有工具需要创建");
            return null;
        }
        
        logger.info("开始创建工具提供者: 工具数量={}, userId={}", mcpServerNames.size(), userId);
        
        List<McpClient> successfulClients = new ArrayList<>();
        List<String> failedTools = new ArrayList<>();
        
        // 逐个创建工具客户端（MCPGatewayService会自动处理容器准备）
        for (String mcpServerName : mcpServerNames) {
            try {
                McpClient client = createSingleToolClient(mcpServerName, userId, toolPresetParams);
                if (client != null) {
                    successfulClients.add(client);
                    logger.info("工具客户端创建成功: {}", mcpServerName);
                } else {
                    failedTools.add(mcpServerName);
                    logger.warn("工具客户端创建失败: {}", mcpServerName);
                }
            } catch (Exception e) {
                failedTools.add(mcpServerName);
                logger.error("工具客户端创建异常: {}", mcpServerName, e);
            }
        }
        
        // 结果统计
        logger.info("工具提供者创建完成 - 成功: {}, 失败: {}", successfulClients.size(), failedTools.size());
        if (!failedTools.isEmpty()) {
            logger.warn("失败的工具列表: {}", failedTools);
        }
        
        if (successfulClients.isEmpty()) {
            logger.error("所有工具客户端创建失败");
            return null;
        }
        
        return new McpToolProvider(successfulClients);
    }
    
    /**
     * 执行Agent对话流程准备
     */
    private ConversationFlowResult prepareAgentConversationFlow(String agentId, String userId) {
        try {
            return conversationFlowService.startAgentConversationFlow(agentId, userId);
        } catch (Exception e) {
            logger.error("Agent对话流程准备异常: agentId={}, userId={}", agentId, userId, e);
            return new ConversationFlowResult(false, "流程准备异常：" + e.getMessage(), null, null);
        }
    }
    
    /**
     * 创建单个工具客户端（核心改造：智能URL获取）
     */
    private McpClient createSingleToolClient(String mcpServerName, String userId, 
                                           Map<String, Map<String, Map<String, String>>> toolPresetParams) {
        try {
            logger.debug("创建工具客户端: 工具={}, userId={}", mcpServerName, userId);
            
            // 关键改造：让MCPGatewayService自动处理工具类型判断和容器准备
            String sseUrl = mcpGatewayService.getSSEUrl(mcpServerName, userId);
            
            logger.info("获取工具连接URL成功: 工具={}, URL={}", mcpServerName, maskUrl(sseUrl));
            
            // 创建HTTP传输层
            McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(sseUrl)
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofMinutes(30))
                .build();
            
            // 创建MCP客户端
            McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
            
            // 设置预设参数
            setPresetParameters(mcpClient, mcpServerName, toolPresetParams);
            
            return mcpClient;
            
        } catch (Exception e) {
            logger.error("创建工具客户端失败: {}", mcpServerName, e);
            throw new BusinessException("工具连接失败：" + mcpServerName + " - " + e.getMessage());
        }
    }
    
    /**
     * 设置工具预设参数
     */
    private void setPresetParameters(McpClient mcpClient, String mcpServerName, 
                                   Map<String, Map<String, Map<String, String>>> toolPresetParams) {
        if (toolPresetParams == null || !toolPresetParams.containsKey(mcpServerName)) {
            return;
        }
        
        try {
            Map<String, Map<String, String>> serverParams = toolPresetParams.get(mcpServerName);
            for (Map.Entry<String, Map<String, String>> entry : serverParams.entrySet()) {
                String toolName = entry.getKey();
                Map<String, String> params = entry.getValue();
                
                if (params != null && !params.isEmpty()) {
                    mcpClient.setPresetParameters(toolName, 
                        params.entrySet().stream()
                            .map(p -> new PresetParameter(p.getKey(), p.getValue()))
                            .collect(Collectors.toList())
                    );
                    
                    logger.debug("设置工具预设参数: 服务={}, 工具={}, 参数数量={}", 
                               mcpServerName, toolName, params.size());
                }
            }
        } catch (Exception e) {
            logger.warn("设置工具预设参数失败: {}", mcpServerName, e);
        }
    }
    
    /**
     * 记录工具创建结果
     */
    private void logToolCreationResult(List<McpClient> successfulClients, List<String> failedTools) {
        logger.info("工具提供者创建完成 - 成功: {}, 失败: {}", 
                   successfulClients.size(), failedTools.size());
        
        if (!failedTools.isEmpty()) {
            logger.warn("失败的工具列表: {}", failedTools);
        }
    }
    
    /**
     * 屏蔽URL中的敏感信息
     */
    private String maskUrl(String url) {
        if (url == null) return null;
        return url.replaceAll("api_key=[^&]*", "api_key=***");
    }
    
    /**
     * 兼容性方法：保持向后兼容
     */
    public ToolProvider createToolProvider(List<String> mcpServerNames,
                                         Map<String, Map<String, Map<String, String>>> toolPresetParams) {
        logger.warn("使用兼容性方法创建工具提供者，无法支持用户隔离工具");
        return createToolProvider(mcpServerNames, toolPresetParams, null, null);
    }
}
```

### 3. 对话服务集成点修改

#### 3.1 关键修改：在AgentMessageHandler中传递用户ID

现有的对话流程已经很完善，主要修改点是在创建工具提供者时传递用户上下文：

```java
// 修改 AgentMessageHandler.provideTools() 方法
@Component
public class AgentMessageHandler extends AbstractMessageHandler {
    
    private final AgentToolManager agentToolManager;
    
    @Override
    protected ToolProvider provideTools(ChatContext chatContext) {
        // 关键改造：传递用户ID给工具管理器
        return agentToolManager.createToolProvider(
            chatContext.getMcpServerNames(),
            chatContext.getAgent().getToolPresetParams(),
            chatContext.getUserId()  // 新增：传递用户ID
        );
    }
}
```

#### 3.2 ConversationAppService调用点检查

确保在创建ChatContext时包含用户ID：

```java
@Service 
public class ConversationAppService {
    
    /**
     * 准备对话环境时确保用户ID正确传递
     */
    private ChatContext prepareEnvironmentWithModel(String sessionId, String userId, ...) {
        // ... 现有逻辑 ...
        
        ChatContext chatContext = new ChatContext();
        chatContext.setUserId(userId);  // 确保用户ID被设置
        chatContext.setMcpServerNames(mcpServerNames);
        // ... 其他设置 ...
        
        return chatContext;
    }
}
```

### 4. 工具类型识别机制

#### 4.1 ToolEntity 扩展

```java
@Entity
@Table(name = "tools")
public class ToolEntity extends BaseEntity {
    
    // 现有字段...
    
    /**
     * 是否为全局工具
     * true: 全局工具，部署在全局MCP Gateway，所有用户共享
     * false: 用户隔离工具，需要部署到用户专属容器
     */
    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = false;
    
    /**
     * 工具服务名称（用于MCP连接）
     */
    @Column(name = "server_name", nullable = false)
    private String serverName;
    
    public boolean isGlobal() {
        return Boolean.TRUE.equals(this.isGlobal);
    }
    
    public boolean requiresUserContainer() {
        return !isGlobal();
    }
    
    // getter/setter...
}
```

#### 4.2 ToolDomainService 扩展

```java
@Service
public class ToolDomainService {
    
    /**
     * 根据服务名称获取工具实体
     */
    public ToolEntity getToolByServerName(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return null;
        }
        
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery()
            .eq(ToolEntity::getServerName, serverName)
            .eq(ToolEntity::getEnabled, true);
            
        return toolRepository.selectOne(wrapper);
    }
    
    /**
     * 批量获取工具类型信息
     */
    public Map<String, Boolean> getToolGlobalStatus(List<String> serverNames) {
        if (serverNames == null || serverNames.isEmpty()) {
            return Collections.emptyMap();
        }
        
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.<ToolEntity>lambdaQuery()
            .in(ToolEntity::getServerName, serverNames)
            .eq(ToolEntity::getEnabled, true)
            .select(ToolEntity::getServerName, ToolEntity::getIsGlobal);
            
        List<ToolEntity> tools = toolRepository.selectList(wrapper);
        
        return tools.stream()
            .collect(Collectors.toMap(
                ToolEntity::getServerName,
                ToolEntity::isGlobal
            ));
    }
    
    /**
     * 获取Agent的用户隔离工具列表
     */
    public List<String> getAgentUserIsolatedTools(String agentId) {
        List<String> allTools = getAgentToolNames(agentId);
        Map<String, Boolean> toolGlobalStatus = getToolGlobalStatus(allTools);
        
        return allTools.stream()
            .filter(toolName -> !toolGlobalStatus.getOrDefault(toolName, true))
            .collect(Collectors.toList());
    }
}
```

### 5. 错误处理和容错机制

#### 5.1 分层错误处理

```java
public class ContainerToolIntegrationException extends BusinessException {
    
    public enum ErrorType {
        CONTAINER_NOT_FOUND("容器不存在"),
        CONTAINER_UNHEALTHY("容器不健康"), 
        TOOL_DEPLOYMENT_FAILED("工具部署失败"),
        MCP_CONNECTION_FAILED("MCP连接失败"),
        NETWORK_ERROR("网络错误");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
    }
    
    private final ErrorType errorType;
    private final String userId;
    private final String toolName;
    
    public ContainerToolIntegrationException(ErrorType errorType, String userId, String toolName, String message) {
        super(String.format("[%s] 用户:%s, 工具:%s - %s", errorType.description, userId, toolName, message));
        this.errorType = errorType;
        this.userId = userId;
        this.toolName = toolName;
    }
    
    // getter方法...
}
```

#### 5.2 智能重试机制

```java
@Component
public class ContainerToolRetryHandler {
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;
    
    /**
     * 带重试的容器操作
     */
    public <T> T executeWithRetry(String operation, Callable<T> callable) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
                logger.warn("操作失败，尝试重试: 操作={}, 尝试次数={}/{}", operation, attempt, MAX_RETRY_ATTEMPTS, e);
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    Thread.sleep(RETRY_DELAY_MS * attempt); // 指数退避
                }
            }
        }
        
        throw new BusinessException("操作失败，已重试" + MAX_RETRY_ATTEMPTS + "次: " + operation, lastException);
    }
}
```

### 6. 性能优化策略

#### 6.1 容器信息缓存

```java
@Service
public class ContainerInfoCacheService {
    
    private final Cache<String, ContainerInfo> containerInfoCache = 
        Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .refreshAfterWrite(2, TimeUnit.MINUTES)
            .build();
    
    /**
     * 获取缓存的容器信息
     */
    public ContainerInfo getCachedContainerInfo(String userId) {
        return containerInfoCache.get(userId, this::loadContainerInfo);
    }
    
    /**
     * 加载容器信息
     */
    private ContainerInfo loadContainerInfo(String userId) {
        return containerAppService.getUserContainerInfo(userId);
    }
    
    /**
     * 清除用户缓存
     */
    public void evictCache(String userId) {
        containerInfoCache.invalidate(userId);
    }
}
```

#### 6.2 MCP客户端连接池

```java
@Component
public class McpClientPool {
    
    private final Map<String, McpClient> clientPool = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * 获取或创建MCP客户端
     */
    public McpClient getOrCreateClient(String cacheKey, Supplier<McpClient> clientFactory) {
        // 读锁：快速路径
        lock.readLock().lock();
        try {
            McpClient existing = clientPool.get(cacheKey);
            if (existing != null) {
                return existing;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        // 写锁：创建客户端
        lock.writeLock().lock();
        try {
            // 双重检查
            McpClient existing = clientPool.get(cacheKey);
            if (existing != null) {
                return existing;
            }
            
            McpClient newClient = clientFactory.get();
            clientPool.put(cacheKey, newClient);
            return newClient;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 清理无效客户端
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void cleanupInvalidClients() {
        lock.writeLock().lock();
        try {
            clientPool.entrySet().removeIf(entry -> {
                try {
                    // 检查客户端是否仍然有效
                    return !isClientValid(entry.getValue());
                } catch (Exception e) {
                    logger.warn("检查MCP客户端有效性失败: {}", entry.getKey(), e);
                    return true; // 移除有问题的客户端
                }
            });
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private boolean isClientValid(McpClient client) {
        // 实现客户端有效性检查逻辑
        return true;
    }
}
```

### 7. 监控和可观测性

#### 7.1 关键指标监控

```java
@Component
public class ContainerToolMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter toolCreationSuccessCounter;
    private final Counter toolCreationFailureCounter;
    private final Timer containerPrepareTimer;
    
    public ContainerToolMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.toolCreationSuccessCounter = Counter.builder("tool.creation.success")
            .description("Successful tool creations")
            .register(meterRegistry);
        this.toolCreationFailureCounter = Counter.builder("tool.creation.failure")
            .description("Failed tool creations") 
            .register(meterRegistry);
        this.containerPrepareTimer = Timer.builder("container.prepare.duration")
            .description("Container preparation time")
            .register(meterRegistry);
    }
    
    public void recordToolCreationSuccess(String toolType) {
        toolCreationSuccessCounter.increment(Tags.of("type", toolType));
    }
    
    public void recordToolCreationFailure(String toolType, String reason) {
        toolCreationFailureCounter.increment(Tags.of("type", toolType, "reason", reason));
    }
    
    public Timer.Sample startContainerPrepareTimer() {
        return Timer.start(meterRegistry);
    }
}
```

#### 7.2 结构化日志

```java
@Component
public class ContainerToolLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(ContainerToolLogger.class);
    
    /**
     * 记录容器准备开始
     */
    public void logContainerPrepareStart(String userId, String agentId, List<String> tools) {
        MDC.put("userId", userId);
        MDC.put("agentId", agentId);
        MDC.put("operation", "container.prepare.start");
        
        logger.info("开始容器准备流程: 工具数量={}, 工具列表={}", tools.size(), tools);
        
        MDC.clear();
    }
    
    /**
     * 记录工具创建结果
     */
    public void logToolCreationResult(String userId, String toolName, boolean success, String reason) {
        MDC.put("userId", userId);
        MDC.put("toolName", toolName);
        MDC.put("operation", "tool.creation.result");
        MDC.put("success", String.valueOf(success));
        
        if (success) {
            logger.info("工具创建成功");
        } else {
            logger.warn("工具创建失败: 原因={}", reason);
        }
        
        MDC.clear();
    }
}
```

### 8. 测试策略

#### 8.1 单元测试

```java
@ExtendWith(MockitoExtension.class)
class MCPGatewayServiceTest {
    
    @Mock
    private ContainerAppService containerAppService;
    
    @Mock 
    private MCPGatewayProperties properties;
    
    @InjectMocks
    private MCPGatewayService mcpGatewayService;
    
    @Test
    void testGetSSEUrl_GlobalTool() {
        // Given
        when(properties.getBaseUrl()).thenReturn("http://global-gateway:8005");
        when(properties.getApiKey()).thenReturn("test-key");
        
        // When
        String result = mcpGatewayService.getSSEUrl("test-tool", "user123", true);
        
        // Then
        assertEquals("http://global-gateway:8005/test-tool/sse/sse?api_key=test-key", result);
    }
    
    @Test
    void testGetSSEUrl_UserTool_Success() {
        // Given
        ContainerAppService.ContainerInfo containerInfo = 
            new ContainerAppService.ContainerInfo("192.168.1.100", 8005, true, ContainerStatus.RUNNING, "container-123");
        when(containerAppService.getUserContainerInfo("user123")).thenReturn(containerInfo);
        when(properties.getApiKey()).thenReturn("test-key");
        
        // When
        String result = mcpGatewayService.getSSEUrl("test-tool", "user123", false);
        
        // Then
        assertEquals("http://192.168.1.100:8005/test-tool/sse/sse?api_key=test-key", result);
    }
    
    @Test
    void testGetSSEUrl_UserTool_ContainerNotFound() {
        // Given
        when(containerAppService.getUserContainerInfo("user123")).thenReturn(null);
        
        // When & Then
        assertThrows(BusinessException.class, () -> {
            mcpGatewayService.getSSEUrl("test-tool", "user123", false);
        });
    }
}
```

#### 8.2 集成测试

```java
@SpringBootTest
@Testcontainers
class ContainerToolIntegrationTest {
    
    @Container
    static final GenericContainer<?> mcpGateway = new GenericContainer<>("mcp-gateway:latest")
        .withExposedPorts(8005)
        .waitingFor(Wait.forHttp("/health"));
    
    @Autowired
    private AgentToolManager agentToolManager;
    
    @Test
    void testCreateToolProvider_MixedTools() {
        // Given
        List<String> tools = Arrays.asList("global-tool", "user-tool");
        String userId = "test-user";
        String agentId = "test-agent";
        
        // When
        ToolProvider result = agentToolManager.createToolProvider(tools, null, userId, agentId);
        
        // Then
        assertNotNull(result);
        // 验证工具提供者的功能...
    }
}
```

### 9. 部署和运维指南

#### 9.1 容器镜像准备

```dockerfile
# 用户容器镜像示例
FROM node:18-alpine

# 安装MCP网关
RUN npm install -g @mcp/gateway

# 设置工作目录
WORKDIR /app

# 复制MCP网关配置
COPY mcp-gateway.config.json /app/

# 暴露端口
EXPOSE 8005

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8005/health || exit 1

# 启动命令
CMD ["mcp-gateway", "--config", "/app/mcp-gateway.config.json"]
```

#### 9.2 环境配置

```yaml
# application.yml 扩展配置
mcp:
  gateway:
    # 全局MCP Gateway配置
    base-url: ${MCP_GATEWAY_URL:http://localhost:8005}
    api-key: ${MCP_GATEWAY_API_KEY:123456}
    
    # 用户容器MCP配置
    user-container:
      # 用户容器中MCP服务的端口
      port: ${USER_CONTAINER_MCP_PORT:8005}
      # 连接超时配置
      connect-timeout: ${USER_CONTAINER_CONNECT_TIMEOUT:30000}
      read-timeout: ${USER_CONTAINER_READ_TIMEOUT:60000}
      # 健康检查配置
      health-check:
        enabled: ${USER_CONTAINER_HEALTH_CHECK_ENABLED:true}
        interval: ${USER_CONTAINER_HEALTH_CHECK_INTERVAL:60000}
        timeout: ${USER_CONTAINER_HEALTH_CHECK_TIMEOUT:5000}

# 容器管理配置扩展
container:
  management:
    # 用户容器默认镜像
    default-image: ${USER_CONTAINER_DEFAULT_IMAGE:agentx/user-mcp-gateway:latest}
    # 端口分配范围
    port-range:
      start: ${CONTAINER_PORT_RANGE_START:30000}
      end: ${CONTAINER_PORT_RANGE_END:40000}
    # 资源限制
    resources:
      memory: ${CONTAINER_MEMORY_LIMIT:512m}
      cpu: ${CONTAINER_CPU_LIMIT:0.5}
```

### 10. 实施检查清单

#### 10.1 代码修改检查

- [ ] `MCPGatewayService.getSSEUrl()` 方法支持工具类型参数
- [ ] `AgentToolManager.createToolProvider()` 方法支持用户上下文
- [ ] `ContainerAppService.getUserContainerInfo()` 方法返回详细容器信息
- [ ] `ToolEntity` 实体添加 `isGlobal` 字段
- [ ] `ToolDomainService` 添加工具类型查询方法
- [ ] 对话服务调用点传递用户和Agent上下文
- [ ] 异常处理和重试机制完善
- [ ] 监控指标和日志记录添加

#### 10.2 测试验证检查

- [ ] 全局工具对话测试通过
- [ ] 用户隔离工具对话测试通过
- [ ] 混合工具对话测试通过
- [ ] 容器不存在时自动创建测试通过
- [ ] 容器不健康时错误处理测试通过
- [ ] 工具部署失败时降级测试通过
- [ ] 并发用户容器创建测试通过
- [ ] 负载测试和性能验证通过

#### 10.3 运维准备检查

- [ ] 用户容器镜像构建和推送
- [ ] 监控告警规则配置
- [ ] 日志收集和分析配置
- [ ] 容器资源限制配置
- [ ] 网络安全策略配置
- [ ] 备份和恢复流程准备
- [ ] 故障处理手册编写
- [ ] 容器清理策略验证

---

## 总结

通过上述详细的技术实现方案，我们可以实现Agent与容器的深度集成，支持全局工具和用户隔离工具的混合使用。关键改进包括：

1. **智能URL路由**：根据工具类型自动选择全局或用户容器连接
2. **容器生命周期管理**：自动创建、健康检查、故障恢复
3. **工具部署自动化**：按需部署工具到用户容器
4. **性能优化**：缓存、连接池、异步处理
5. **可观测性**：监控、日志、告警

这套方案确保了Agent对话流程的流畅性，同时维护了用户数据的隔离性和系统的高可用性。
# Docker容器管理系统

## 概述

AgentX平台的Docker容器管理系统，实现了用户隔离的MCP网关容器环境，支持动态工具部署和智能对话功能。

## 系统架构

### 核心组件

1. **容器管理模块** (`org.xhy.domain.container`)
   - 领域实体：`ContainerEntity` - 容器信息管理
   - 领域服务：`ContainerDomainService` - 容器生命周期管理
   - 仓储接口：`ContainerRepository` - 数据访问抽象

2. **Docker服务** (`org.xhy.infrastructure.docker`)
   - `DockerService` - Docker容器操作封装
   - `PullImageResultCallback` - 镜像拉取回调

3. **应用服务层** (`org.xhy.application.container`)
   - `ContainerAppService` - 容器应用服务
   - `McpGatewayService` - MCP网关集成服务
   - `ContainerMonitorService` - 容器监控服务
   - `ContainerIntegrationAppService` - 对话流程集成服务

4. **管理界面** (`app/(main)/admin/containers`)
   - 符合AgentX设计风格的容器管理界面
   - 支持容器列表、状态监控、操作管理

### 数据模型

#### 容器实体 (ContainerEntity)
```java
- id: 容器ID
- name: 容器名称
- userId: 用户ID
- type: 容器类型 (用户容器/审核容器)
- status: 容器状态 (创建中/运行中/已停止/错误状态/删除中/已删除)
- dockerContainerId: Docker容器ID
- image: 容器镜像
- internalPort: 内部端口 (默认8080)
- externalPort: 外部映射端口 (30000-40000随机分配)
- ipAddress: 容器IP地址
- cpuUsage: CPU使用率
- memoryUsage: 内存使用率
- volumePath: 数据卷路径
- errorMessage: 错误信息
```

#### 容器类型
- **用户容器** (USER): 每个用户的专属MCP网关容器
- **审核容器** (REVIEW): 临时容器，用于工具审核

#### 容器状态
- **创建中** (CREATING): 容器正在创建
- **运行中** (RUNNING): 容器正常运行
- **已停止** (STOPPED): 容器已停止
- **错误状态** (ERROR): 容器出现错误
- **删除中** (DELETING): 容器正在删除
- **已删除** (DELETED): 容器已删除

## 核心功能

### 1. 容器生命周期管理

#### 创建用户容器
```java
ContainerDTO container = containerAppService.createUserContainer(userId);
```

#### 容器状态检查
```java
ContainerHealthStatus health = containerAppService.checkUserContainerHealth(userId);
```

#### 容器操作
```java
// 启动容器
containerAppService.startContainer(containerId, operator);

// 停止容器
containerAppService.stopContainer(containerId, operator);

// 删除容器
containerAppService.deleteContainer(containerId, operator);
```

### 2. MCP网关集成

#### 工具部署
```java
List<ToolConfig> toolConfigs = ...;
ToolDeploymentResult result = mcpGatewayService.deployToolsToUserContainer(userId, toolConfigs);
```

#### 工具状态查询
```java
List<ToolStatus> tools = mcpGatewayService.getDeployedToolsStatus(userId);
```

### 3. 容器监控

#### 定时监控任务
- **状态检查**: 每5分钟检查容器运行状态
- **资源监控**: 每2分钟更新CPU/内存使用率

#### 健康检查
- Docker容器状态检查
- MCP网关可用性检查
- 网络连接验证

### 4. 端口管理

#### 自动端口分配
- 外部端口范围: 30000-40000
- 随机分配未占用端口
- 端口占用检测和冲突避免

#### 网络配置
- 支持容器IP直连
- 支持端口映射访问
- 自动网络配置检测

## 对话Agent流程集成

### 1. 容器状态检查
```java
ContainerCheckResult result = containerIntegrationAppService.checkUserContainerStatus(userId);
```

### 2. 容器创建（如需要）
```java
ContainerCreationResult result = containerIntegrationAppService.createUserContainerIfNeeded(userId);
```

### 3. 工具部署
```java
ToolDeploymentResult result = containerIntegrationAppService.deployToolsToUserWorkspace(userId, toolConfigs);
```

### 4. 对话启动
容器和工具准备就绪后，可以开始Agent对话。

## API接口

### 管理员接口

#### 容器管理
- `GET /admin/containers` - 分页获取容器列表
- `GET /admin/containers/statistics` - 获取容器统计信息
- `POST /admin/containers/review` - 创建审核容器
- `POST /admin/containers/{id}/start` - 启动容器
- `POST /admin/containers/{id}/stop` - 停止容器
- `DELETE /admin/containers/{id}` - 删除容器

### 用户接口

#### 容器管理
- `GET /api/containers/user` - 获取用户容器
- `POST /api/containers/user` - 创建用户容器
- `GET /api/containers/user/health` - 检查容器健康状态

## 配置项

### 应用配置 (application.yml)
```yaml
agentx:
  container:
    docker-host: unix:///var/run/docker.sock
    user-volume-base-path: /docker/users
    default-mcp-gateway-image: ghcr.io/lucky-aeon/mcp-gateway:latest
    monitor-interval: 300000  # 5分钟
    stats-update-interval: 120000  # 2分钟
```

### 容器模板配置
```java
ContainerTemplate template = ContainerTemplate.getDefaultMcpGatewayTemplate();
- 镜像: ghcr.io/lucky-aeon/mcp-gateway:latest
- 内部端口: 8080
- CPU限制: 1.0核
- 内存限制: 512MB
- 网络模式: bridge
- 重启策略: unless-stopped
```

## 数据库

### 表结构
```sql
CREATE TABLE user_containers (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    type INTEGER NOT NULL,
    status INTEGER NOT NULL,
    docker_container_id VARCHAR(100),
    image VARCHAR(200) NOT NULL,
    internal_port INTEGER NOT NULL,
    external_port INTEGER,
    ip_address VARCHAR(45),
    cpu_usage DECIMAL(5,2),
    memory_usage DECIMAL(5,2),
    volume_path VARCHAR(500),
    env_config TEXT,
    container_config TEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 索引
- `idx_user_containers_user_id` - 用户ID索引
- `idx_user_containers_type` - 容器类型索引
- `idx_user_containers_status` - 容器状态索引
- `idx_user_containers_external_port` - 外部端口唯一索引

## 部署要求

### 系统依赖
- Docker Engine
- Java 17+
- PostgreSQL 12+

### Maven依赖
```xml
<dependency>
    <groupId>com.github.docker-java</groupId>
    <artifactId>docker-java-core</artifactId>
    <version>3.3.4</version>
</dependency>
<dependency>
    <groupId>com.github.docker-java</groupId>
    <artifactId>docker-java-transport-httpclient5</artifactId>
    <version>3.3.4</version>
</dependency>
```

### 权限要求
- Docker socket访问权限
- 文件系统读写权限 (`/docker/users/`)
- 网络端口绑定权限 (30000-40000)

## 监控和日志

### 监控指标
- 容器总数 / 运行中容器数
- CPU / 内存使用率
- 端口分配情况
- 错误率统计

### 日志级别
- `INFO`: 容器创建、启动、停止等重要操作
- `WARN`: 容器状态异常、健康检查失败
- `ERROR`: Docker操作失败、网络错误
- `DEBUG`: 资源使用率更新、详细状态检查

## 故障排除

### 常见问题

1. **Docker连接失败**
   - 检查Docker Engine是否运行
   - 验证socket权限: `/var/run/docker.sock`

2. **端口分配失败**
   - 检查端口范围是否被占用
   - 调整端口分配范围配置

3. **容器创建失败**
   - 检查镜像是否存在
   - 验证网络配置
   - 查看Docker日志

4. **MCP网关不可用**
   - 检查容器网络连接
   - 验证端口映射
   - 查看网关健康状态

### 调试命令
```bash
# 查看容器状态
docker ps -a

# 查看容器日志
docker logs <container-id>

# 检查端口占用
netstat -tulpn | grep :8080

# 测试网关连接
curl http://localhost:<external-port>/health
```

## 安全考虑

### 容器隔离
- 每个用户独立容器
- 资源限制和配额
- 网络隔离和端口控制

### 数据安全
- 用户数据卷隔离
- 敏感信息加密存储
- 访问权限控制

### 操作审计
- 容器操作日志记录
- 用户操作追踪
- 异常行为监控

## 性能优化

### 资源管理
- 容器资源限制
- 镜像缓存优化
- 数据卷性能调优

### 监控优化
- 批量状态检查
- 异步操作处理
- 缓存机制使用

### 扩展性
- 水平扩展支持
- 负载均衡配置
- 分布式部署
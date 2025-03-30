# 服务商模型与Agent集成设计文档

## 1. 系统架构概述

本设计实现用户自行添加服务商和模型，并在Agent中灵活使用的功能。系统支持官方和用户自定义的服务商/模型，并根据不同来源采用不同的计费和调用逻辑。

## 2. 数据库设计

### 2.1 服务商表(providers)

```sql
CREATE TABLE providers (
    id VARCHAR(36) PRIMARY KEY COMMENT '服务商ID',
    user_id VARCHAR(36) COMMENT '用户ID，官方服务商为NULL',
    code VARCHAR(50) NOT NULL COMMENT '服务商代码，例如：OPENAI、ANTHROPIC',
    name VARCHAR(100) NOT NULL COMMENT '服务商名称',
    config TEXT NOT NULL COMMENT '服务商配置参数(加密存储)，如：{"api_key": "xxx", "api_secret": "xxx"}',
    is_official BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否官方服务商',
    status BOOLEAN NOT NULL DEFAULT TRUE COMMENT '状态：TRUE-启用，FALSE-禁用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',
    UNIQUE KEY uk_providers_user_code (user_id, code)
);

-- 创建索引
CREATE INDEX idx_providers_user_id ON providers(user_id);
CREATE INDEX idx_providers_code ON providers(code);
```

### 2.2 模型表(models)

```sql
CREATE TABLE models (
    id VARCHAR(36) PRIMARY KEY COMMENT '模型ID',
    user_id VARCHAR(36) COMMENT '用户ID，官方模型为NULL',
    provider_id VARCHAR(36) NOT NULL COMMENT '服务商ID',
    code VARCHAR(100) NOT NULL COMMENT '模型代码，例如：gpt-4、claude-3',
    name VARCHAR(100) NOT NULL COMMENT '模型名称',
    description TEXT COMMENT '模型描述',
    type VARCHAR(20) NOT NULL COMMENT '模型类型：CHAT-对话模型，EMBEDDING-嵌入模型',
    max_context_length INT COMMENT '最大上下文长度',
    config JSON COMMENT '模型默认配置参数',
    is_official BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否官方模型',
    status BOOLEAN NOT NULL DEFAULT TRUE COMMENT '状态：TRUE-启用，FALSE-禁用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',
    UNIQUE KEY uk_models_user_provider_code (user_id, provider_id, code)
);

-- 创建索引
CREATE INDEX idx_models_user_id ON models(user_id);
CREATE INDEX idx_models_provider_id ON models(provider_id);
CREATE INDEX idx_models_type ON models(type);
```

### 2.3 扩展Agent工作区表

```sql
-- 在现有agent_workspace表添加模型ID字段
ALTER TABLE agent_workspace 
ADD COLUMN model_id VARCHAR(36) COMMENT '用户选择的模型ID，NULL表示使用Agent默认模型';
```

## 3. 核心功能流程

### 3.1 服务商管理

1. **系统预设服务商类型**：
   - 系统代码中硬编码支持的服务商类型列表(OpenAI、Anthropic等)
   - 预设每种服务商所需的配置参数结构

2. **用户添加服务商**：
   - 用户从支持的类型中选择服务商
   - 填写配置参数(API密钥等)
   - 系统加密存储配置信息
   - 标记为非官方服务商(is_official=false)

3. **服务商验证**：
   - 添加/更新服务商配置时进行连接测试
   - 验证API密钥的有效性

### 3.2 模型管理

1. **系统预设模型**：
   - 官方支持的服务商预设常用模型
   - 包括模型类型、最大上下文长度等信息

2. **用户添加模型**：
   - 选择已配置的服务商
   - 设置模型代码、名称、类型等信息
   - 配置最大上下文长度、默认参数等
   - 标记为非官方模型(is_official=false)

### 3.3 Agent创建与模型关联

1. **创建/编辑Agent**：
   - 选择模型(官方或自定义)
   - 配置模型参数(temperature等)
   - 添加推荐模型字段，提示用户最佳模型选择

2. **ModelConfig扩展**：
   ```java
   public class ModelConfig {
       // 现有字段
       private String modelName;       // 模型名称
       private Double temperature;     // 温度参数
       private Double topP;            // Top P参数 
       private Integer maxTokens;      // 最大令牌数
       private Boolean loadMemory;     // 是否启用记忆功能
       private String systemMessage;   // 系统消息
       
       // 新增字段
       private String recommendedModelId; // 推荐的模型ID
   }
   ```

### 3.4 用户使用Agent流程

1. **添加Agent到工作区**：
   - 用户从市场添加Agent到工作区
   - 记录在agent_workspace表，不立即设置model_id

2. **使用Agent对话**：
   - 首次对话时，系统检查：
     * Agent推荐的模型
     * 用户是否有兼容的自定义模型
   - 提示用户选择模型来源
   - 记录选择到agent_workspace表的model_id字段

3. **模型服务调用**：
   - 根据agent_workspace中的model_id获取模型信息
   - 如果model_id为空，使用Agent默认/推荐的模型
   - 检查模型关联的服务商是官方还是非官方
   - 官方服务商：通过平台调用并计费
   - 非官方服务商：使用用户自己的API密钥调用

## 4. 服务接口设计

### 4.1 服务商管理接口

```
GET    /api/providers                 # 获取服务商列表(包括官方和用户自己的)
GET    /api/providers/supported       # 获取系统支持的服务商类型列表
POST   /api/providers                 # 添加服务商配置  
PUT    /api/providers/{id}            # 更新服务商配置
DELETE /api/providers/{id}            # 删除服务商配置
POST   /api/providers/{id}/test       # 测试服务商配置连接
```

### 4.2 模型管理接口

```
GET    /api/models                    # 获取模型列表(包括官方和用户自己的)
GET    /api/models/provider/{id}      # 获取指定服务商的模型列表
POST   /api/models                    # 添加模型配置
PUT    /api/models/{id}               # 更新模型配置
DELETE /api/models/{id}               # 删除模型配置
```

### 4.3 Agent模型选择接口

```
GET    /api/agents/{id}/recommended-model  # 获取Agent推荐的模型
PUT    /api/workspace/agents/{agentId}/model  # 设置工作区Agent使用的模型
```

## 5. 核心类设计

### 5.1 服务商实体类

```java
@Entity
@Table(name = "providers")
public class ProviderEntity {
    @Id
    private String id;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "code")
    private String code;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "config")
    private String config;  // 加密存储
    
    @Column(name = "is_official")
    private Boolean isOfficial;
    
    @Column(name = "status")
    private Boolean status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    // getter/setter方法
}
```

### 5.2 模型实体类

```java
@Entity
@Table(name = "models")
public class ModelEntity {
    @Id
    private String id;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "provider_id")
    private String providerId;
    
    @Column(name = "code")
    private String code;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "type")
    private String type;  // CHAT, EMBEDDING
    
    @Column(name = "max_context_length")
    private Integer maxContextLength;
    
    @Column(name = "config")
    @Convert(converter = JsonToStringConverter.class)
    private Map<String, Object> config;
    
    @Column(name = "is_official")
    private Boolean isOfficial;
    
    @Column(name = "status")
    private Boolean status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    // getter/setter方法
}
```

### 5.3 服务商工厂类

```java
@Service
public class ProviderFactory {
    private final Map<String, ProviderHandler> handlers = new HashMap<>();
    private final ProviderRepository providerRepository;
    private final ModelRepository modelRepository;
    private final EncryptionService encryptionService;
    
    @Autowired
    public ProviderFactory(ProviderRepository providerRepository, 
                          ModelRepository modelRepository,
                          EncryptionService encryptionService) {
        this.providerRepository = providerRepository;
        this.modelRepository = modelRepository;
        this.encryptionService = encryptionService;
        
        // 注册支持的服务商处理器
        registerProviders();
    }
    
    private void registerProviders() {
        handlers.put("OPENAI", new OpenAIProviderHandler());
        handlers.put("ANTHROPIC", new AnthropicProviderHandler());
        // 添加更多服务商处理器
    }
    
    public ProviderHandler getHandler(String providerCode) {
        return handlers.get(providerCode.toUpperCase());
    }
    
    public List<String> getSupportedProviders() {
        return new ArrayList<>(handlers.keySet());
    }
    
    public ModelLLMClient createClient(String modelId, String userId) {
        // 查找模型
        ModelEntity model = modelRepository.findById(modelId);
        if (model == null) {
            throw new EntityNotFoundException("Model not found: " + modelId);
        }
        
        // 查找服务商
        ProviderEntity provider = providerRepository.findById(model.getProviderId());
        if (provider == null) {
            throw new EntityNotFoundException("Provider not found: " + model.getProviderId());
        }
        
        // 获取处理器
        ProviderHandler handler = getHandler(provider.getCode());
        if (handler == null) {
            throw new UnsupportedOperationException("Unsupported provider: " + provider.getCode());
        }
        
        // 解密配置
        String decryptedConfig = encryptionService.decrypt(provider.getConfig());
        
        // 创建客户端
        return handler.createClient(model.getCode(), decryptedConfig, model.getConfig());
    }
}
```

## 6. 模型调用流程

### 6.1 会话创建

```java
@Service
public class ConversationService {
    
    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private WorkspaceRepository workspaceRepository;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @Autowired
    private ProviderFactory providerFactory;
    
    @Autowired
    private BillingService billingService;
    
    // 创建会话
    public String createSession(SessionCreateRequest request, String userId) {
        // 获取Agent
        AgentEntity agent = agentRepository.findById(request.getAgentId());
        if (agent == null) {
            throw new EntityNotFoundException("Agent not found");
        }
        
        // 获取用户的Agent工作区记录
        WorkspaceEntity workspace = workspaceRepository.findByUserAndAgent(userId, request.getAgentId());
        if (workspace == null) {
            throw new EntityNotFoundException("Agent not in workspace");
        }
        
        // 确定使用的模型ID
        String modelId = workspace.getModelId();
        
        // 如果工作区未设置模型ID，使用Agent默认/推荐的模型
        if (modelId == null) {
            ModelConfig modelConfig = agent.getModelConfig();
            if (modelConfig.getRecommendedModelId() != null) {
                modelId = modelConfig.getRecommendedModelId();
            } else {
                // 查找模型名称对应的官方模型
                ModelEntity model = modelRepository.findOfficialByCode(modelConfig.getModelName());
                if (model != null) {
                    modelId = model.getId();
                } else {
                    throw new BusinessException("No suitable model found");
                }
            }
            
            // 更新工作区记录
            workspace.setModelId(modelId);
            workspaceRepository.save(workspace);
        }
        
        // 创建会话
        SessionEntity session = new SessionEntity();
        session.setId(UUID.randomUUID().toString());
        session.setTitle(request.getTitle());
        session.setUserId(userId);
        session.setAgentId(request.getAgentId());
        session.setModelId(modelId);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        
        sessionRepository.save(session);
        return session.getId();
    }
    
    // 发送消息
    public MessageResponse sendMessage(String sessionId, MessageRequest request, String userId) {
        // 获取会话
        SessionEntity session = sessionRepository.findById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new EntityNotFoundException("Session not found or access denied");
        }
        
        // 获取会话使用的模型
        String modelId = session.getModelId();
        ModelEntity model = modelRepository.findById(modelId);
        
        // 获取模型关联的服务商
        ProviderEntity provider = providerRepository.findById(model.getProviderId());
        
        // 创建大模型客户端
        ModelLLMClient client = providerFactory.createClient(modelId, userId);
        
        // 获取对话历史
        List<Message> history = getConversationHistory(sessionId);
        
        // 调用模型服务获取回复
        String assistantResponse = client.chat(request.getContent(), history);
        
        // 保存用户消息和回复
        saveMessage(sessionId, "user", request.getContent());
        MessageEntity assistantMessage = saveMessage(sessionId, "assistant", assistantResponse);
        
        // 如果使用官方服务，记录使用量并计费
        if (provider.getIsOfficial()) {
            // 计算token数量
            int inputTokens = TokenCalculator.countTokens(request.getContent());
            int outputTokens = TokenCalculator.countTokens(assistantResponse);
            
            // 记录使用量
            billingService.recordUsage(userId, session.getAgentId(), modelId, 
                    inputTokens, outputTokens);
        }
        
        return new MessageResponse(assistantMessage.getId(), assistantResponse);
    }
}
```

## 7. 安全设计

### 7.1 服务商配置加密

```java
@Service
public class EncryptionService {
    
    @Value("${app.encryption.key}")
    private String encryptionKey;
    
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;
    
    public EncryptionService() throws Exception {
        // 初始化加密/解密器
        // ...
    }
    
    public String encrypt(String plainText) {
        try {
            byte[] encrypted = encryptCipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    public String decrypt(String encryptedText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = decryptCipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
```

### 7.2 访问控制

```java
@Service
public class ProviderService {
    
    @Autowired
    private ProviderRepository providerRepository;
    
    @Autowired
    private EncryptionService encryptionService;
    
    // 获取用户可见的服务商列表
    public List<ProviderDTO> getUserVisibleProviders(String userId) {
        // 获取官方服务商
        List<ProviderEntity> officialProviders = providerRepository.findByIsOfficial(true);
        
        // 获取用户自己的服务商
        List<ProviderEntity> userProviders = providerRepository.findByUserId(userId);
        
        // 合并结果
        List<ProviderDTO> result = new ArrayList<>();
        officialProviders.forEach(p -> result.add(convertToDTO(p, false)));
        userProviders.forEach(p -> result.add(convertToDTO(p, true)));
        
        return result;
    }
    
    // 添加服务商配置
    public String addProvider(ProviderDTO dto, String userId) {
        // 检查服务商代码是否支持
        if (!isSupportedProvider(dto.getCode())) {
            throw new BusinessException("Unsupported provider code");
        }
        
        // 验证配置参数
        validateConfig(dto.getCode(), dto.getConfig());
        
        // 创建服务商实体
        ProviderEntity provider = new ProviderEntity();
        provider.setId(UUID.randomUUID().toString());
        provider.setUserId(userId);
        provider.setCode(dto.getCode());
        provider.setName(dto.getName());
        
        // 加密存储配置
        provider.setConfig(encryptionService.encrypt(dto.getConfig()));
        
        provider.setIsOfficial(false);
        provider.setStatus(true);
        provider.setCreatedAt(LocalDateTime.now());
        provider.setUpdatedAt(LocalDateTime.now());
        
        providerRepository.save(provider);
        return provider.getId();
    }
    
    // 其他方法...
}
```

## 8. 总结

本设计实现了服务商、模型与Agent的完整集成方案，具有以下特点：

1. **支持多样化服务来源**：同时支持平台官方服务和用户自定义服务
2. **灵活的模型选择**：用户可为每个Agent选择不同的模型来源
3. **安全的配置管理**：加密存储API密钥，确保用户凭证安全
4. **无缝的用户体验**：自动选择合适的服务来源，提升用户体验
5. **清晰的系统架构**：遵循DDD设计原则，实现各层次职责分离

通过这种设计，系统可以支持用户灵活添加和使用各种AI服务商和模型，同时保持良好的用户体验和系统安全性。 
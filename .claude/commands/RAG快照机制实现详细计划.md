# RAG快照机制完整实现计划

## 业务逻辑确认

### RAG版本类型和行为
- **0.0.1版本 = REFERENCE类型**：
  - 只有RAG创建者可以安装
  - 数据动态引用原始RAG，会随原始数据变化而更新
  - 显示原始RAG的实时信息
  
- **其他发布版本 = SNAPSHOT类型**：
  - 所有用户都可以从市场安装
  - 数据是完全独立的快照副本，安装后固定不变
  - 显示安装时的快照信息

### 关键业务约束
- **用户删除RAG时**：原始RAG数据 + rag_versions + rag_version_files + rag_version_documents 都会被删除
- **user_rags不删除**：已安装的用户记录保留，必须依然能正常使用
- **数据隔离要求**：SNAPSHOT类型必须在安装时完全复制所有数据，确保删除后依然可用

## 需要实现的具体功能

### 第一步：创建用户级快照表

#### 1.1 创建数据库表
**文件位置**：`/AgentX/src/main/resources/db/migration/V20250722_001__add_user_rag_snapshot_tables.sql`

```sql
-- 用户RAG文件快照表
CREATE TABLE user_rag_files (
    id VARCHAR(36) PRIMARY KEY,
    user_rag_id VARCHAR(36) NOT NULL,           -- 关联user_rags表
    
    -- 文件信息快照（从rag_version_files复制）
    original_file_id VARCHAR(36) NOT NULL,      -- 原始文件ID（仅标识）
    file_name VARCHAR(255) NOT NULL,            -- 文件名
    file_size BIGINT DEFAULT 0,                 -- 文件大小
    file_type VARCHAR(50),                      -- 文件类型
    file_path VARCHAR(500),                     -- 文件存储路径
    
    -- 处理状态快照
    process_status INTEGER,                     -- 处理状态
    embedding_status INTEGER,                   -- 向量化状态
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 用户RAG文档快照表  
CREATE TABLE user_rag_documents (
    id VARCHAR(36) PRIMARY KEY,
    user_rag_id VARCHAR(36) NOT NULL,           -- 关联user_rags表
    user_rag_file_id VARCHAR(36) NOT NULL,      -- 关联user_rag_files表
    
    -- 文档内容快照（从rag_version_documents复制）
    original_document_id VARCHAR(36),           -- 原始文档单元ID（仅标识）
    content TEXT NOT NULL,                      -- 文档内容
    page INTEGER,                               -- 页码
    
    -- 向量数据存储在向量数据库中，使用 user_rag_id 作为命名空间
    vector_id VARCHAR(36),                      -- 向量ID
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);


```

#### 1.2 创建实体类
**文件位置**：`/AgentX/src/main/java/org/xhy/domain/rag/model/UserRagFileEntity.java`

```java
@TableName("user_rag_files")
public class UserRagFileEntity extends BaseEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userRagId;
    private String originalFileId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String filePath;
    private Integer processStatus;
    private Integer embeddingStatus;
    // getters and setters
}
```

**文件位置**：`/AgentX/src/main/java/org/xhy/domain/rag/model/UserRagDocumentEntity.java`

```java
@TableName("user_rag_documents")
public class UserRagDocumentEntity extends BaseEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userRagId;
    private String userRagFileId;
    private String originalDocumentId;
    private String content;
    private Integer page;
    private String vectorId;
    // getters and setters
}
```

#### 1.3 创建Repository接口
**文件位置**：`/AgentX/src/main/java/org/xhy/domain/rag/repository/UserRagFileRepository.java`
**文件位置**：`/AgentX/src/main/java/org/xhy/domain/rag/repository/UserRagDocumentRepository.java`

### 第二步：修改安装逻辑

#### 2.1 修改UserRagDomainService.installRag()方法
**文件位置**：`/AgentX/src/main/java/org/xhy/domain/rag/service/UserRagDomainService.java`

**修改逻辑**：
```java
@Transactional
public UserRagEntity installRag(String userId, String ragVersionId) {
    // 现有逻辑...验证、检查重复等
    
    // 确定安装类型
    InstallType installType = determineInstallType(userId, ragVersion);
    
    // 创建基本安装记录
    UserRagEntity userRag = createBasicUserRag(userId, ragVersion, installType);
    userRagRepository.insert(userRag);
    
    // 如果是SNAPSHOT类型，复制完整数据
    if (installType == InstallType.SNAPSHOT) {
        copyVersionSnapshotToUser(userRag.getId(), ragVersionId);
    }
    
    return userRag;
}

// 新增方法：复制版本快照到用户快照
private void copyVersionSnapshotToUser(String userRagId, String ragVersionId) {
    // 复制文件快照
    copyVersionFilesToUser(userRagId, ragVersionId);
    // 复制文档快照
    copyVersionDocumentsToUser(userRagId, ragVersionId);
    // 复制向量数据到新的命名空间
    copyVectorDataToUser(userRagId, ragVersionId);
}
```

#### 2.2 新增UserRagSnapshotService
**文件位置**：`/AgentX/src/main/java/org/xhy/domain/rag/service/UserRagSnapshotService.java`

```java
@Service
public class UserRagSnapshotService {
    
    private final UserRagFileRepository userRagFileRepository;
    private final UserRagDocumentRepository userRagDocumentRepository;
    private final RagVersionFileRepository ragVersionFileRepository;
    private final RagVersionDocumentRepository ragVersionDocumentRepository;
    
    /** 复制版本文件快照到用户快照 */
    @Transactional
    public void copyVersionFilesToUser(String userRagId, String ragVersionId) {
        // 查询版本文件快照
        List<RagVersionFileEntity> versionFiles = getVersionFiles(ragVersionId);
        
        // 转换并保存为用户文件快照
        for (RagVersionFileEntity versionFile : versionFiles) {
            UserRagFileEntity userFile = convertToUserRagFile(versionFile, userRagId);
            userRagFileRepository.insert(userFile);
        }
    }
    
    /** 复制版本文档快照到用户快照 */
    @Transactional  
    public void copyVersionDocumentsToUser(String userRagId, String ragVersionId) {
        // 查询版本文档快照
        List<RagVersionDocumentEntity> versionDocuments = getVersionDocuments(ragVersionId);
        
        // 建立文件ID映射关系
        Map<String, String> fileIdMapping = buildFileIdMapping(userRagId, ragVersionId);
        
        // 转换并保存为用户文档快照
        for (RagVersionDocumentEntity versionDoc : versionDocuments) {
            UserRagDocumentEntity userDoc = convertToUserRagDocument(versionDoc, userRagId, fileIdMapping);
            userRagDocumentRepository.insert(userDoc);
        }
    }
    
    /** 复制向量数据到用户命名空间 */
    @Transactional
    public void copyVectorDataToUser(String userRagId, String ragVersionId) {
        // 调用向量数据库服务，从 ragVersionId 命名空间复制到 userRagId 命名空间
        // TODO: 具体实现依赖向量数据库的API
    }
}
```

### 第三步：修改数据访问逻辑

#### 3.1 完善RagDataAccessService
**文件位置**：`/AgentX/src/main/java/org/xhy/domain/rag/service/RagDataAccessService.java`

**修改现有方法实现**：
```java
/** 获取用户可用的RAG文件列表 */
public List<FileDetailEntity> getRagFiles(String userId, String userRagId) {
    UserRagEntity userRag = getUserRag(userId, userRagId);

    if (userRag.isReferenceType()) {
        // REFERENCE类型：从原始数据集获取最新文件
        return getRealTimeFiles(userRag.getOriginalRagId(), userId);
    } else {
        // SNAPSHOT类型：从用户快照获取固定文件
        return getUserSnapshotFiles(userRagId);
    }
}

/** 获取用户可用的RAG文档单元列表 */
public List<DocumentUnitEntity> getRagDocuments(String userId, String userRagId) {
    UserRagEntity userRag = getUserRag(userId, userRagId);

    if (userRag.isReferenceType()) {
        // REFERENCE类型：从原始数据集获取最新文档
        return getRealTimeDocuments(userRag.getOriginalRagId(), userId);
    } else {
        // SNAPSHOT类型：从用户快照获取固定文档
        return getUserSnapshotDocuments(userRagId);
    }
}

// 新增方法：获取用户快照文件
private List<FileDetailEntity> getUserSnapshotFiles(String userRagId) {
    LambdaQueryWrapper<UserRagFileEntity> wrapper = Wrappers.<UserRagFileEntity>lambdaQuery()
        .eq(UserRagFileEntity::getUserRagId, userRagId)
        .orderByDesc(UserRagFileEntity::getCreatedAt);
    
    List<UserRagFileEntity> userFiles = userRagFileRepository.selectList(wrapper);
    
    // 转换为FileDetailEntity格式
    return userFiles.stream()
        .map(this::convertToFileDetailEntity)
        .collect(Collectors.toList());
}

// 新增方法：获取用户快照文档
private List<DocumentUnitEntity> getUserSnapshotDocuments(String userRagId) {
    LambdaQueryWrapper<UserRagDocumentEntity> wrapper = Wrappers.<UserRagDocumentEntity>lambdaQuery()
        .eq(UserRagDocumentEntity::getUserRagId, userRagId)
        .orderByDesc(UserRagDocumentEntity::getCreatedAt);
    
    List<UserRagDocumentEntity> userDocs = userRagDocumentRepository.selectList(wrapper);
    
    // 转换为DocumentUnitEntity格式
    return userDocs.stream()
        .map(this::convertToDocumentUnitEntity)
        .collect(Collectors.toList());
}
```

### 第四步：修改显示逻辑

#### 4.1 扩展UserRagDTO
**文件位置**：`/AgentX/src/main/java/org/xhy/application/rag/dto/UserRagDTO.java`

```java
/** 安装类型 */
private InstallType installType;

/** 是否为引用类型 */
public boolean isReferenceType() {
    return installType != null && installType.isReference();
}

/** 是否为快照类型 */ 
public boolean isSnapshotType() {
    return installType != null && installType.isSnapshot();
}

// getter and setter for installType
```

#### 4.2 修改UserRagAssembler
**文件位置**：`/AgentX/src/main/java/org/xhy/application/rag/assembler/UserRagAssembler.java`

```java
/** Convert Entity to DTO using BeanUtils */
public static UserRagDTO toDTO(UserRagEntity entity) {
    if (entity == null) {
        return null;
    }

    UserRagDTO dto = new UserRagDTO();
    BeanUtils.copyProperties(entity, dto);
    
    return dto;
}

/** 处理REFERENCE类型：获取原始RAG的实时信息 */
public static UserRagDTO enrichWithReferenceInfo(UserRagEntity entity, RagQaDatasetEntity originalRag, String creatorNickname) {
    if (entity == null) {
        return null;
    }

    UserRagDTO dto = toDTO(entity);
    
    // 使用原始RAG的实时信息
    dto.setName(originalRag.getName());
    dto.setDescription(originalRag.getDescription());
    dto.setIcon(originalRag.getIcon());
    
    // 设置统计信息和创建者信息
    dto.setFileCount(originalRag.getFileCount());
    dto.setDocumentCount(originalRag.getDocumentCount());
    dto.setCreatorNickname(creatorNickname);
    dto.setCreatorId(originalRag.getUserId());
    
    return dto;
}

/** 处理SNAPSHOT类型：使用快照数据 */
public static UserRagDTO enrichWithSnapshotInfo(UserRagEntity entity, Integer fileCount, Integer documentCount, String creatorNickname, String creatorId) {
    if (entity == null) {
        return null;
    }

    UserRagDTO dto = toDTO(entity);
    
    // 使用entity中的快照信息（name、description、icon已经是快照数据）
    // 只补充统计信息和创建者信息
    dto.setFileCount(fileCount);
    dto.setDocumentCount(documentCount);
    dto.setCreatorNickname(creatorNickname);
    dto.setCreatorId(creatorId);
    
    return dto;
}
```

#### 4.3 修改RagMarketAppService
**文件位置**：`/AgentX/src/main/java/org/xhy/application/rag/RagMarketAppService.java`

```java
/** 获取用户安装的RAG列表 */
public Page<UserRagDTO> getUserInstalledRags(String userId, Integer page, Integer pageSize, String keyword) {
    IPage<UserRagEntity> entityPage = userRagDomainService.listInstalledRags(userId, page, pageSize, keyword);

    // 转换为DTO
    List<UserRagDTO> dtoList = new ArrayList<>();

    for (UserRagEntity entity : entityPage.getRecords()) {
        UserRagDTO dto;
        
        if (entity.isReferenceType()) {
            // REFERENCE类型：获取原始RAG的实时信息
            dto = enrichWithReferenceInfo(entity);
        } else {
            // SNAPSHOT类型：使用快照数据
            dto = enrichWithSnapshotInfo(entity);
        }
        
        dtoList.add(dto);
    }

    // 创建DTO分页对象
    Page<UserRagDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
    dtoPage.setRecords(dtoList);

    return dtoPage;
}

/** 处理REFERENCE类型的信息丰富 */
private UserRagDTO enrichWithReferenceInfo(UserRagEntity entity) {
    try {
        // 获取原始RAG的实时信息
        RagQaDatasetEntity originalRag = ragQaDatasetDomainService.getDataset(entity.getOriginalRagId(), entity.getUserId());
        String creatorNickname = getUserNickname(originalRag.getUserId());
        
        return UserRagAssembler.enrichWithReferenceInfo(entity, originalRag, creatorNickname);
    } catch (Exception e) {
        // 如果原始RAG不存在，返回基本信息
        return UserRagAssembler.toDTO(entity);
    }
}

/** 处理SNAPSHOT类型的信息丰富 */
private UserRagDTO enrichWithSnapshotInfo(UserRagEntity entity) {
    try {
        // 获取快照的统计信息（从用户快照表统计或从安装记录获取）
        Integer fileCount = getUserRagFileCount(entity.getId());
        Integer documentCount = getUserRagDocumentCount(entity.getId());
        
        // 获取创建者信息（从原始版本信息获取，如果版本已删除则使用空值）
        String creatorNickname = null;
        String creatorId = null;
        try {
            RagVersionEntity ragVersion = ragVersionDomainService.getRagVersion(entity.getRagVersionId());
            creatorNickname = getUserNickname(ragVersion.getUserId());
            creatorId = ragVersion.getUserId();
        } catch (Exception e) {
            // 版本已删除，忽略创建者信息
        }
        
        return UserRagAssembler.enrichWithSnapshotInfo(entity, fileCount, documentCount, creatorNickname, creatorId);
    } catch (Exception e) {
        return UserRagAssembler.toDTO(entity);
    }
}

/** 获取用户RAG的文件数量 */
private Integer getUserRagFileCount(String userRagId) {
    LambdaQueryWrapper<UserRagFileEntity> wrapper = Wrappers.<UserRagFileEntity>lambdaQuery()
        .eq(UserRagFileEntity::getUserRagId, userRagId);
    return Math.toIntExact(userRagFileRepository.selectCount(wrapper));
}

/** 获取用户RAG的文档数量 */
private Integer getUserRagDocumentCount(String userRagId) {
    LambdaQueryWrapper<UserRagDocumentEntity> wrapper = Wrappers.<UserRagDocumentEntity>lambdaQuery()
        .eq(UserRagDocumentEntity::getUserRagId, userRagId);
    return Math.toIntExact(userRagDocumentRepository.selectCount(wrapper));
}
```

### 第五步：向量数据处理

#### 5.1 向量数据复制
**需要调研**：当前向量数据库的存储方式和命名空间机制
**实现方案**：
- 发布版本时：向量数据存储在 `rag_version_id` 命名空间
- 用户安装时：复制向量数据到 `user_rag_id` 命名空间
- 检索时：根据安装类型选择对应的命名空间

#### 5.2 修改向量检索逻辑
**需要找到**：当前的向量检索服务位置
**修改内容**：根据userRagId确定使用的向量命名空间

### 第六步：数据清理和维护

#### 6.1 用户卸载RAG时的清理
**修改位置**：`UserRagDomainService.uninstallRag()`
**新增逻辑**：
- 删除user_rag_files表数据
- 删除user_rag_documents表数据  
- 删除对应的向量数据

#### 6.2 数据完整性保护
**实现内容**：
- 软删除保护：用户RAG快照数据只能软删除
- 数据验证：安装时验证版本快照数据完整性
- 错误恢复：安装失败时的数据回滚机制

### 第七步：前端适配

#### 7.1 更新类型定义
**文件位置**：`/agentx-frontend-plus/types/rag-publish.ts`
```typescript
export interface UserRag {
  // 现有字段...
  installType: 'REFERENCE' | 'SNAPSHOT';
}
```

#### 7.2 前端显示优化
**修改组件**：已安装RAG相关的展示组件
**显示区别**：
- REFERENCE类型：显示"实时版本"标识，数据会更新
- SNAPSHOT类型：显示"固定版本 v1.0.0"标识，数据固定

## 验收标准

### 功能验收
1. **REFERENCE类型**：
   - 显示原始RAG的实时信息
   - 原始RAG更新时，显示信息同步更新
   - 对话检索使用原始RAG的最新数据

2. **SNAPSHOT类型**：
   - 显示安装时的快照信息
   - 原始RAG更新或删除都不影响显示
   - 对话检索使用独立的快照数据
   - 原始RAG删除后依然可以正常使用

3. **数据隔离**：
   - 不同用户安装同一RAG拥有独立数据副本
   - 一个用户的操作不影响其他用户的数据

### 性能验收
1. 安装SNAPSHOT类型RAG的时间在可接受范围内
2. 快照数据的检索性能与原始数据相当
3. 大量快照数据不影响系统整体性能

### 数据一致性验收
1. 安装过程中的数据复制完整无遗漏
2. 向量数据与文档数据保持对应关系
3. 异常情况下的数据回滚机制正常工作